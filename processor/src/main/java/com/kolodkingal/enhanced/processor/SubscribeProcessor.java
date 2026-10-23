package com.kolodkingal.enhanced.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes the registration index of the mod's common-event listeners at compile time: every {@code @Subscribe} method
 * of a {@code @Subscriber} class becomes one line of {@code platform.event.generated.GeneratedSubscriptions} (both
 * sides, and dedicated-server-only ones) or {@code GeneratedClientSubscriptions} (client-only), so nothing scans
 * classes at run time.
 *
 * <p>Order. Within one event type and priority, the listeners are registered in the order NeoForge called them in
 * 3.8.2, as recorded in {@code docs/port/listener-order.txt} (option {@code enhanced.listenerOrder}): the server's
 * recording first, a client-only listener after the listener it followed in the client's recording. A listener the
 * recording does not know comes after the known ones, by class and then method name. The generated {@code INDEX} lists
 * every listener in that order ({@code type|PRIORITY|receiveCanceled|SIDE|class#method}), for the order GameTest.
 *
 * <p>Checks (compile errors): the method is {@code public static void} with one parameter whose type is a concrete
 * common event ({@code platform.event} or {@code platform.event.client}); its class is a public {@code @Subscriber}
 * (nested classes public and static); a client event only in a {@code Side.CLIENT} subscriber.
 */
@SupportedAnnotationTypes({SubscribeProcessor.SUBSCRIBE, SubscribeProcessor.SUBSCRIBER})
@SupportedOptions(SubscribeProcessor.ORDER_OPTION)
public final class SubscribeProcessor extends AbstractProcessor {
    static final String EVENT_PACKAGE = "com.kolodkingal.enhanced.platform.event";
    static final String SUBSCRIBE = EVENT_PACKAGE + ".Subscribe";
    static final String SUBSCRIBER = EVENT_PACKAGE + ".Subscriber";
    static final String ORDER_OPTION = "enhanced.listenerOrder";
    static final String OUT_PACKAGE = EVENT_PACKAGE + ".generated";
    static final String[] PRIORITIES = {"HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST"};

    private Elements elements;
    private Messager messager;
    private Filer filer;
    private boolean written;
    private final List<Sub> subs = new ArrayList<>();

    /** One listener: its event's binary name and source name, priority, receiveCanceled, side, owner and method. */
    record Sub(String eventBinary, String eventSource, String priority, boolean receiveCanceled, String side,
               String ownerBinary, String ownerSource, String method, Element origin) {
        String key() {
            return ownerBinary + "#" + method;
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elements = env.getElementUtils();
        messager = env.getMessager();
        filer = env.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement subscribe = elements.getTypeElement(SUBSCRIBE);
        if (subscribe == null) {
            return false;
        }
        List<Sub> found = new ArrayList<>();
        for (Element element : round.getElementsAnnotatedWith(subscribe)) {
            Sub sub = check(element);
            if (sub != null) {
                found.add(sub);
            }
        }
        if (written) {
            for (Sub sub : found) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@Subscribe in generated code is not supported",
                        sub.origin());
            }
            return false;
        }
        subs.addAll(found);
        if (!round.processingOver()) {
            written = true;
            write();
        }
        return false;
    }

    private Sub check(Element element) {
        if (element.getKind() != ElementKind.METHOD) {
            return error(element, "@Subscribe is for methods");
        }
        ExecutableElement method = (ExecutableElement) element;
        Set<Modifier> mods = method.getModifiers();
        if (!mods.contains(Modifier.PUBLIC) || !mods.contains(Modifier.STATIC)
                || method.getReturnType().getKind() != TypeKind.VOID || method.getParameters().size() != 1) {
            return error(element, "a @Subscribe method is public static void and takes one common event");
        }
        TypeElement owner = (TypeElement) method.getEnclosingElement();
        AnnotationMirror subscriber = mirror(owner, SUBSCRIBER);
        if (subscriber == null) {
            return error(element, "the class of a @Subscribe method must be a @Subscriber: "
                    + owner.getQualifiedName());
        }
        for (Element e = owner; e instanceof TypeElement t; e = e.getEnclosingElement()) {
            if (!t.getModifiers().contains(Modifier.PUBLIC)
                    || (t.getNestingKind() == NestingKind.MEMBER && !t.getModifiers().contains(Modifier.STATIC))) {
                return error(element, "a @Subscriber class (and every class around it) must be public and static: "
                        + t.getQualifiedName());
            }
        }
        VariableElement param = method.getParameters().get(0);
        TypeMirror type = param.asType();
        if (!(type instanceof DeclaredType declared) || !((TypeElement) declared.asElement()).getTypeParameters()
                .isEmpty()) {
            return error(element, "the parameter of a @Subscribe method is a common event type");
        }
        TypeElement event = (TypeElement) declared.asElement();
        String eventPackage = elements.getPackageOf(event).getQualifiedName().toString();
        if (!eventPackage.equals(EVENT_PACKAGE) && !eventPackage.startsWith(EVENT_PACKAGE + ".")) {
            return error(element, "not a common event (platform.event): " + event.getQualifiedName());
        }
        if (event.getModifiers().contains(Modifier.ABSTRACT)) {
            return error(element, "listen to a concrete event, not the abstract " + event.getQualifiedName());
        }
        String side = enumValue(subscriber, "side", "BOTH");
        if (eventPackage.startsWith(EVENT_PACKAGE + ".client") && !side.equals("CLIENT")) {
            return error(element, "a client event needs a @Subscriber(side = Side.CLIENT) class");
        }
        AnnotationMirror annotation = mirror(method, SUBSCRIBE);
        String priority = enumValue(annotation, "priority", "NORMAL");
        boolean receiveCanceled = Boolean.parseBoolean(String.valueOf(value(annotation, "receiveCanceled", false)));
        return new Sub(elements.getBinaryName(event).toString(), event.getQualifiedName().toString(), priority,
                receiveCanceled, side, elements.getBinaryName(owner).toString(), owner.getQualifiedName().toString(),
                method.getSimpleName().toString(), element);
    }

    private Sub error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
        return null;
    }

    private static AnnotationMirror mirror(Element element, String type) {
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (((TypeElement) m.getAnnotationType().asElement()).getQualifiedName().contentEquals(type)) {
                return m;
            }
        }
        return null;
    }

    private static Object value(AnnotationMirror mirror, String name, Object fallback) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues()
                .entrySet()) {
            if (e.getKey().getSimpleName().contentEquals(name)) {
                return e.getValue().getValue();
            }
        }
        return fallback;
    }

    private static String enumValue(AnnotationMirror mirror, String name, String fallback) {
        Object v = value(mirror, name, null);
        return v instanceof VariableElement constant ? constant.getSimpleName().toString() : fallback;
    }

    // ---- order ----

    /** The recorded rank of each listener ("class#method") per event type and priority. */
    private Map<String, Map<String, Integer>> recordedRanks() {
        String path = processingEnv.getOptions().get(ORDER_OPTION);
        Map<String, Map<String, Integer>> ranks = new HashMap<>();
        if (path == null || path.isBlank()) {
            return ranks;
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file)) {
            messager.printMessage(Diagnostic.Kind.WARNING, "no listener-order recording at " + file
                    + ": listeners are ordered by class and method name");
            return ranks;
        }
        try {
            ListenerOrder order = ListenerOrder.parse(Files.readAllLines(file, StandardCharsets.UTF_8));
            order.merged().forEach((group, keys) -> {
                Map<String, Integer> rank = new HashMap<>();
                for (int i = 0; i < keys.size(); i++) {
                    rank.putIfAbsent(keys.get(i), i);
                }
                ranks.put(group, rank);
            });
        } catch (IOException | IllegalArgumentException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "cannot read the listener-order recording " + file + ": "
                    + e.getMessage());
        }
        return ranks;
    }

    private List<Sub> ordered() {
        Map<String, Map<String, Integer>> ranks = recordedRanks();
        Map<String, Integer> priorityRank = new HashMap<>();
        for (int i = 0; i < PRIORITIES.length; i++) {
            priorityRank.put(PRIORITIES[i], i);
        }
        Comparator<Sub> order = Comparator.comparing(Sub::eventBinary)
                .thenComparing(s -> priorityRank.get(s.priority()))
                .thenComparing(s -> ranks.getOrDefault(ListenerOrder.group(s.eventBinary(), s.priority()), Map.of())
                        .getOrDefault(s.key(), Integer.MAX_VALUE))
                .thenComparing(Sub::ownerBinary)
                .thenComparing(Sub::method);
        List<Sub> sorted = new ArrayList<>(subs);
        sorted.sort(order);
        return sorted;
    }

    // ---- output ----

    private void write() {
        List<Sub> sorted = ordered();
        Map<String, List<Sub>> bySide = new TreeMap<>();
        for (String side : List.of("BOTH", "SERVER", "CLIENT")) {
            bySide.put(side, new ArrayList<>());
        }
        for (Sub sub : sorted) {
            bySide.get(sub.side()).add(sub);
        }
        Element[] origins = subs.stream().map(Sub::origin).toArray(Element[]::new);
        StringBuilder main = new StringBuilder();
        main.append("package ").append(OUT_PACKAGE).append(";\n\n")
                .append("import ").append(EVENT_PACKAGE).append(".EventHub;\n")
                .append("import ").append(EVENT_PACKAGE).append(".Priority;\n")
                .append("import ").append(EVENT_PACKAGE).append(".Side;\n\n")
                .append("import java.util.List;\n\n")
                .append("/** Generated by {@code SubscribeProcessor}: the mod's common-event listeners. ")
                .append("Do not edit. */")
                .append('\n')
                .append("public final class GeneratedSubscriptions {\n")
                .append("    /** Every listener ({@code type|PRIORITY|receiveCanceled|SIDE|class#method}), ")
                .append("in order. */")
                .append('\n')
                .append("    public static final List<String> INDEX = List.of(");
        boolean first = true;
        for (Sub sub : sorted) {
            main.append(first ? "\n" : ",\n").append("            \"").append(sub.eventBinary()).append('|')
                    .append(sub.priority()).append('|').append(sub.receiveCanceled()).append('|').append(sub.side())
                    .append('|').append(sub.key()).append('"');
            first = false;
        }
        main.append(");\n\n")
                .append("    private GeneratedSubscriptions() {\n    }\n\n")
                .append("    /** Registers the listeners of this physical side ({@code Events.registerGenerated}). */")
                .append('\n')
                .append("    public static void register(Side physicalSide) {\n")
                .append("        registerBoth();\n")
                .append("        if (physicalSide == Side.CLIENT) {\n")
                .append("            GeneratedClientSubscriptions.register();\n")
                .append("        } else {\n")
                .append("            registerDedicatedServer();\n")
                .append("        }\n")
                .append("    }\n\n");
        method(main, "private static void registerBoth()", bySide.get("BOTH"));
        main.append('\n');
        method(main, "private static void registerDedicatedServer()", bySide.get("SERVER"));
        main.append("}\n");

        StringBuilder client = new StringBuilder();
        client.append("package ").append(OUT_PACKAGE).append(";\n\n")
                .append("import ").append(EVENT_PACKAGE).append(".EventHub;\n")
                .append("import ").append(EVENT_PACKAGE).append(".Priority;\n\n")
                .append("/** Generated by {@code SubscribeProcessor}: the client-only listeners. Do not edit. */\n")
                .append("public final class GeneratedClientSubscriptions {\n")
                .append("    private GeneratedClientSubscriptions() {\n    }\n\n");
        method(client, "public static void register()", bySide.get("CLIENT"));
        client.append("}\n");

        source(OUT_PACKAGE + ".GeneratedSubscriptions", main, origins);
        source(OUT_PACKAGE + ".GeneratedClientSubscriptions", client, origins);
    }

    private static void method(StringBuilder out, String signature, List<Sub> list) {
        out.append("    ").append(signature).append(" {\n");
        for (Sub sub : list) {
            out.append("        EventHub.of(").append(sub.eventSource()).append(".class).register(Priority.")
                    .append(sub.priority()).append(", ").append(sub.receiveCanceled()).append(",\n")
                    .append("                ").append(sub.ownerSource()).append("::").append(sub.method())
                    .append(", \"").append(sub.key()).append("\");\n");
        }
        out.append("    }\n");
    }

    private void source(String name, CharSequence text, Element[] origins) {
        try {
            JavaFileObject file = filer.createSourceFile(name, origins);
            try (Writer w = file.openWriter()) {
                w.write(text.toString());
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "cannot write " + name + ": " + e.getMessage());
        }
    }

    /** The parsed recording ({@code docs/port/listener-order.txt}); {@code ListenerOrderRecorder} has the format. */
    static final class ListenerOrder {
        /** Per section ("server", "client"), per group ("commonType|PRIORITY"), the listeners in order. */
        private final Map<String, Map<String, List<String>>> sections = new LinkedHashMap<>();

        static String group(String commonType, String priority) {
            return commonType + "|" + priority;
        }

        static ListenerOrder parse(List<String> lines) {
            ListenerOrder order = new ListenerOrder();
            Map<String, List<String>> section = null;
            String common = null;
            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = order.sections.computeIfAbsent(line.substring(1, line.length() - 1),
                            k -> new LinkedHashMap<>());
                    common = null;
                } else if (line.startsWith("event ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 4 || !parts[2].equals("->")) {
                        throw new IllegalArgumentException("bad event line: " + line);
                    }
                    common = parts[3].equals("-") ? null : parts[3];
                } else {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 3 || section == null) {
                        throw new IllegalArgumentException("bad listener line: " + line);
                    }
                    if (common != null) {
                        section.computeIfAbsent(group(common, parts[0]), k -> new ArrayList<>()).add(parts[2]);
                    }
                }
            }
            return order;
        }

        /** The server's order, with every listener only the client knows inserted after its predecessor there. */
        Map<String, List<String>> merged() {
            Map<String, List<String>> server = sections.getOrDefault("server", Map.of());
            Map<String, List<String>> client = sections.getOrDefault("client", Map.of());
            Map<String, List<String>> merged = new LinkedHashMap<>();
            server.forEach((g, keys) -> merged.put(g, new ArrayList<>(keys)));
            client.forEach((g, keys) -> {
                List<String> into = merged.computeIfAbsent(g, k -> new ArrayList<>());
                int at = 0;
                for (String key : keys) {
                    int known = into.indexOf(key);
                    if (known >= 0) {
                        at = known + 1;
                    } else {
                        into.add(at++, key);
                    }
                }
            });
            return merged;
        }
    }
}
