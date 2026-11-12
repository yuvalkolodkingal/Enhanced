import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Makes one Minecraft version's copy of the mod from the shared sources (docs/MULTIVERSION.md):
 * <ul>
 *   <li>version blocks: the lines between {@code //#if >=1.21.4} (or {@code <1.21.4}, {@code ==1.21.4}) and
 *   {@code //#else} / {@code //#endif} are kept when the condition holds for this version and commented out with
 *   {@code //$$ } when it does not; the lines after {@code //#else} the other way round. In the shared sources the
 *   lines of the versions that are not 1.21.1 are written commented, as {@code //$$ code};</li>
 *   <li>renames (below), except in a file that has the line {@code //#norenames} (an overlay written in the
 *   version's own names): such a file still gets the renames of the versions after the one whose folder it is in
 *   (a 1.21.11 file in a 26.3 copy gets 26.3's renames), when the overlay folders and the rename files are listed
 *   in the same order, one per version; {@code //#norenames 1.21.11} in a later version's folder says the file is
 *   written in 1.21.11's names (a copy of a 1.21.11 file, changed for the later version);</li>
 *   <li>relocation: {@code <basePackage>} becomes {@code <basePackage>.<suffix>} (all
 *   but the {@code boot} package, which is not copied), so that every copy has classes of its own in the one jar.</li>
 * </ul>
 */
abstract class Preprocess extends DefaultTask {
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getSource()

    @OutputDirectory
    abstract DirectoryProperty getTarget()

    /**
     * Folders of files that replace the shared file of the same path whole (written as for 1.21.1's package), in
     * order: a later folder wins. A version's copy lists the folders of the versions before it, then its own
     * (1.21.5: 1.21.4's, then 1.21.5's), so that each version only holds what changed since the one before.
     */
    @org.gradle.api.tasks.InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract org.gradle.api.file.ConfigurableFileCollection getOverlays()

    /**
     * Pure renames, applied to every file after the version blocks, file by file in order (the versions before this
     * one first): one {@code regex => replacement} per line ({@code #} comments). For names that only moved or
     * changed (MobSpawnType => EntitySpawnReason). A line {@code @in(<regex>) regex => replacement} applies only to
     * the files whose path (relative to the sources, as for 1.21.1's package) the first regex finds.
     */
    @org.gradle.api.tasks.InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract org.gradle.api.file.ConfigurableFileCollection getRenames()

    @Input
    abstract Property<String> getMinecraft()

    @Input
    abstract Property<String> getSuffix()

    /** The mod's base package (com.kolodkingal.enhanced): what is relocated, and where the sources are. */
    @Input
    abstract Property<String> getBasePackage()

    static int[] parse(String v) {
        def parts = v.split('\\.').collect { it as int }
        while (parts.size() < 3) parts << 0
        return parts as int[]
    }

    static int compare(String a, String b) {
        int[] x = parse(a), y = parse(b)
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) return x[i] <=> y[i]
        }
        return 0
    }

    static boolean holds(String condition, String mc) {
        def m = condition.trim() =~ /^(>=|<=|>|<|==)\s*([0-9.]+)$/
        if (!m.matches()) throw new IllegalArgumentException("bad version condition: " + condition)
        int c = compare(mc, m.group(2))
        switch (m.group(1)) {
            case '>=': return c >= 0
            case '<=': return c <= 0
            case '>': return c > 0
            case '<': return c < 0
            default: return c == 0
        }
    }

    static String uncomment(String line) {
        def m = line =~ /^(\s*)\/\/\$\$ ?(.*)$/
        return m.matches() ? m.group(1) + m.group(2) : line
    }

    static String comment(String line) {
        if (line.trim().isEmpty() || line =~ /^\s*\/\/\$\$/) return line
        def m = line =~ /^(\s*)(.*)$/
        m.matches()
        return m.group(1) + '//$$ ' + m.group(2)
    }

    String process(String text, String file) {
        return process(text, file, -1)
    }

    /** {@code folder}: the index of the overlay folder the file comes from, -1 for a shared file. */
    String process(String text, String file, int folder) {
        String mc = minecraft.get()
        List<String> out = []
        // a stack of [active?, inIf?, condition]
        List<List> stack = []
        int n = 0
        for (String line : text.split('\n', -1)) {
            n++
            def trimmed = line.trim()
            if (trimmed.startsWith('//#if ')) {
                stack << [holds(trimmed.substring(6), mc), true]
                out << line
                continue
            }
            if (trimmed == '//#else') {
                if (stack.isEmpty()) throw new IllegalStateException(file + ':' + n + ': //#else without //#if')
                stack[-1][1] = false
                out << line
                continue
            }
            if (trimmed == '//#endif') {
                if (stack.isEmpty()) throw new IllegalStateException(file + ':' + n + ': //#endif without //#if')
                stack.removeLast()
                out << line
                continue
            }
            boolean on = stack.every { it[0] == it[1] }
            out << (stack.isEmpty() ? line : on ? uncomment(line) : comment(line))
        }
        if (!stack.isEmpty()) throw new IllegalStateException(file + ': //#if without //#endif')
        String s = out.join('\n')
        // (a file written for this version already, with the line //#norenames, is only relocated)
        int from = 0
        def marker = s =~ /(?m)^\s*\/\/#norenames(?:[ \t]+([0-9.]+))?[ \t]*$/
        if (marker.find()) {
            String written = marker.group(1)
            int index = folder
            if (written != null) {
                // (a file written in an earlier version's names: the renames after that version's)
                index = overlays.files.toList().findIndexOf { File d -> d.path.replace('\\', '/').contains('/versions/' + written + '/') }
                if (index < 0) throw new IllegalArgumentException(file + ': //#norenames ' + written + ': no such version in the chain')
            }
            from = index >= 0 && overlays.files.size() == renames.files.size() ? index + 1 : Integer.MAX_VALUE
        }
        for (List rule : renameRules()) {
            if ((rule[2] as int) < from) continue
            if (rule[3] != null && !(file =~ (rule[3] as String)).find()) continue
            s = s.replaceAll(rule[0] as String, rule[1] as String)
        }
        String pkg = basePackage.get()
        String to = pkg + '.' + suffix.get()
        s = s.replaceAll(/\b${java.util.regex.Pattern.quote(pkg)}(?=[.;])(?!\.boot\b)/, java.util.regex.Matcher.quoteReplacement(to))
        return s
    }

    @org.gradle.api.tasks.Internal
    List<List> cachedRules

    /** [regex, replacement, index of the rename file, file regex or null] */
    List<List> renameRules() {
        if (cachedRules == null) {
            List<List> rules = []
            int index = 0
            renames.files.each { File file ->
                int k = index++
                file.readLines('UTF-8').each { String line ->
                    String l = line.trim()
                    if (l.isEmpty() || l.startsWith('#')) return
                    String scope = null
                    if (l.startsWith('@in(')) {
                        int close = l.indexOf(') ')
                        if (close < 0) throw new IllegalArgumentException(file.name + ': bad @in line: ' + line)
                        scope = l.substring(4, close)
                        l = l.substring(close + 2).trim()
                    }
                    int at = l.indexOf(' => ')
                    if (at < 0) throw new IllegalArgumentException(file.name + ': bad rename line: ' + line)
                    rules << [l.substring(0, at), l.substring(at + 4).replace('\\n', '\n'), k, scope]  // (\\n in a replacement is a new line)
                }
            }
            cachedRules = rules
        }
        return cachedRules
    }

    /** The overlay file for {@code path}: the last folder that has it, or null. */
    File overlayFor(String path) {
        int i = overlayIndex(path)
        return i < 0 ? null : new File(overlays.files.toList()[i], path)
    }

    /** The index of the last overlay folder that has {@code path}, or -1. */
    int overlayIndex(String path) {
        int found = -1
        int i = 0
        overlays.files.each { File dir ->
            File f = new File(dir, path)
            if (f.isFile()) found = i
            i++
        }
        return found
    }

    @TaskAction
    void run() {
        File src = source.get().asFile
        File dst = target.get().asFile
        dst.deleteDir()
        String rel = basePackage.get().replace('.', '/') + '/'
        src.eachFileRecurse { File f ->
            if (!f.isFile() || !f.name.endsWith('.java')) return
            String path = src.toPath().relativize(f.toPath()).toString().replace('\\', '/')
            if (!path.startsWith(rel) || path.startsWith(rel + 'boot/')) return
            File out = new File(dst, rel + suffix.get() + '/' + path.substring(rel.length()))
            out.parentFile.mkdirs()
            int folder = overlayIndex(path)
            File from = folder >= 0 ? overlayFor(path) : f
            out.setText(process(from.getText('UTF-8'), path, folder), 'UTF-8')
        }
        // files only the versions' folders have (the last folder that has one wins)
        overlays.files.each { File ov ->
            if (!ov.isDirectory()) return
            ov.eachFileRecurse { File f ->
                if (!f.isFile() || !f.name.endsWith('.java')) return
                String path = ov.toPath().relativize(f.toPath()).toString().replace('\\', '/')
                File out = new File(dst, rel + suffix.get() + '/' + path.substring(rel.length()))
                if (!out.exists()) {
                    out.parentFile.mkdirs()
                    out.setText(process(overlayFor(path).getText('UTF-8'), path, overlayIndex(path)), 'UTF-8')
                }
            }
        }
    }
}
