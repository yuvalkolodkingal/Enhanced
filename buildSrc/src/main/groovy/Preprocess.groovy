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
 *   <li>relocation: {@code <basePackage>} becomes {@code <basePackage>.<suffix>} (all
 *   but the {@code boot} package, which is not copied), so that every copy has classes of its own in the one jar.</li>
 * </ul>
 */
abstract class Preprocess extends DefaultTask {
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getSource()

    @OutputDirectory
    abstract DirectoryProperty getTarget()

    /** Files of this version that replace the shared file of the same path whole (written as for 1.21.1's package). */
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) @org.gradle.api.tasks.Optional
    abstract DirectoryProperty getOverlay()

    /**
     * Pure renames of this version, applied to every file after the version blocks: one {@code regex => replacement}
     * per line ({@code #} comments). For names that only moved or changed (MobSpawnType => EntitySpawnReason).
     */
    @org.gradle.api.tasks.InputFile @PathSensitive(PathSensitivity.RELATIVE) @org.gradle.api.tasks.Optional
    abstract org.gradle.api.file.RegularFileProperty getRenames()

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
        for (List<String> rule : renameRules()) {
            s = s.replaceAll(rule[0], rule[1])
        }
        String pkg = basePackage.get()
        String to = pkg + '.' + suffix.get()
        s = s.replaceAll(/\b${java.util.regex.Pattern.quote(pkg)}(?=[.;])(?!\.boot\b)/, java.util.regex.Matcher.quoteReplacement(to))
        return s
    }

    @org.gradle.api.tasks.Internal
    List<List<String>> cachedRules

    List<List<String>> renameRules() {
        if (cachedRules == null) {
            List<List<String>> rules = []
            if (renames.isPresent()) {
                renames.get().asFile.readLines('UTF-8').each { String line ->
                    String l = line.trim()
                    if (l.isEmpty() || l.startsWith('#')) return
                    int at = l.indexOf(' => ')
                    if (at < 0) throw new IllegalArgumentException('bad rename line: ' + line)
                    rules << [l.substring(0, at), l.substring(at + 4).replace('\\n', '\n')]  // (\\n in a replacement is a new line)
                }
            }
            cachedRules = rules
        }
        return cachedRules
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
            File replacement = overlay.isPresent() ? new File(overlay.get().asFile, path) : null
            File from = replacement != null && replacement.isFile() ? replacement : f
            out.setText(process(from.getText('UTF-8'), path), 'UTF-8')
        }
        if (overlay.isPresent()) {
            // files only this version has
            File ov = overlay.get().asFile
            ov.eachFileRecurse { File f ->
                if (!f.isFile() || !f.name.endsWith('.java')) return
                String path = ov.toPath().relativize(f.toPath()).toString().replace('\\', '/')
                File out = new File(dst, rel + suffix.get() + '/' + path.substring(rel.length()))
                if (!out.exists()) {
                    out.parentFile.mkdirs()
                    out.setText(process(f.getText('UTF-8'), path), 'UTF-8')
                }
            }
        }
    }
}
