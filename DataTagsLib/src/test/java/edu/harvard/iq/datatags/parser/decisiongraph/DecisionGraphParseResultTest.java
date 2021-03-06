package edu.harvard.iq.datatags.parser.decisiongraph;

import edu.harvard.iq.datatags.model.graphs.DecisionGraph;
import edu.harvard.iq.datatags.model.graphs.nodes.AskNode;
import edu.harvard.iq.datatags.model.graphs.nodes.ConsiderNode;
import edu.harvard.iq.datatags.model.graphs.nodes.CallNode;
import edu.harvard.iq.datatags.model.graphs.nodes.EndNode;
import edu.harvard.iq.datatags.model.graphs.nodes.RejectNode;
import edu.harvard.iq.datatags.model.graphs.nodes.SetNode;
import edu.harvard.iq.datatags.model.graphs.nodes.ToDoNode;
import edu.harvard.iq.datatags.model.slots.AggregateSlot;
import edu.harvard.iq.datatags.model.slots.AtomicSlot;
import edu.harvard.iq.datatags.model.slots.CompoundSlot;
import edu.harvard.iq.datatags.model.values.AggregateValue;
import edu.harvard.iq.datatags.model.graphs.Answer;
import edu.harvard.iq.datatags.model.graphs.ConsiderOption;
import edu.harvard.iq.datatags.model.graphs.nodes.PartNode;
import edu.harvard.iq.datatags.model.graphs.nodes.SectionNode;
import edu.harvard.iq.datatags.model.metadata.PolicyModelData;
import edu.harvard.iq.datatags.model.values.CompoundValue;
import edu.harvard.iq.datatags.parser.tagspace.TagSpaceParseResult;
import edu.harvard.iq.datatags.parser.tagspace.TagSpaceParser;
import edu.harvard.iq.datatags.parser.exceptions.DataTagsParseException;
import edu.harvard.iq.datatags.parser.exceptions.SemanticsErrorException;
import edu.harvard.iq.datatags.parser.exceptions.SyntaxErrorException;
import static edu.harvard.iq.datatags.util.CollectionHelper.C;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class DecisionGraphParseResultTest {

    private final CompoundSlot emptyTagSpace = new CompoundSlot("", "");
    private AstNodeIdProvider nodeIdProvider;
    private EndNode endNode;

    public DecisionGraphParseResultTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        nodeIdProvider = new AstNodeIdProvider();
        endNode = new EndNode("[SYN-END]");
        
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTypeIndexBuilding() throws SyntaxErrorException, SemanticsErrorException, DataTagsParseException {
        String typeDef
                = "top: consists of mid1, mid2.\n"
                + "mid1: consists of bottom1, bottom2.\n"
                + "mid2: consists of bottom2, bottom3.\n"
                + "bottom1: one of X11, X12, X13.\n"
                + "bottom2: one of Y21, Y22, Y23.\n"
                + "bottom3: one of Z31, Z32, Z33.";

        TagSpaceParseResult parse = new TagSpaceParser().parse(typeDef);
        CompoundSlot topType = parse.buildType("top").get();

        DecisionGraphCompiler res = new DecisionGraphCompiler();

        res.buildTypeIndex(topType);

        final Map<List<String>, List<String>> typesBySlot = res.fullyQualifiedSlotName;

        Map<List<String>, List<String>> expected = new HashMap<>();
        expected.put(C.list("top", "mid1", "bottom1"), C.list("top", "mid1", "bottom1"));
        expected.put(C.list("top", "mid1", "bottom2"), C.list("top", "mid1", "bottom2"));
        expected.put(C.list("top", "mid2", "bottom2"), C.list("top", "mid2", "bottom2"));
        expected.put(C.list("top", "mid2", "bottom3"), C.list("top", "mid2", "bottom3"));

        expected.put(C.list("mid1", "bottom1"), C.list("top", "mid1", "bottom1"));
        expected.put(C.list("bottom1"), C.list("top", "mid1", "bottom1"));
        expected.put(C.list("mid2", "bottom3"), C.list("top", "mid2", "bottom3"));
        expected.put(C.list("bottom3"), C.list("top", "mid2", "bottom3"));
        expected.put(C.list("mid2", "bottom2"), C.list("top", "mid2", "bottom2"));
        expected.put(C.list("mid1", "bottom2"), C.list("top", "mid1", "bottom2"));

        assertEquals(expected, typesBySlot);
    }

    @Test
    public void todoCallEndTest() throws Exception {

        ToDoNode start = new ToDoNode("fTodo", "this and that");
        final ToDoNode toDoNode = new ToDoNode("sTodo", "bla");
        toDoNode.setNextNode(endNode);
        final PartNode partNode = new PartNode("bla", toDoNode, "ghostbusters");
        CallNode call = new CallNode("fCall", partNode);
        final EndNode end = new EndNode(nodeIdProvider.nextId());
        toDoNode.setNextNode(endNode);
        start.setNextNode(call).setNextNode(end);
        DecisionGraph expected = new DecisionGraph();
        expected.add(start);
        expected.setStart(start);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");

        String code = "[>fTodo< todo: this and that][>fCall< call: ghostbusters][end][-->ghostbusters< {title: bla} [>sTodo< todo: bla]--]";
        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());

        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }

    @Test
    public void todoCallRejectTest() throws Exception {

        ToDoNode start = new ToDoNode("fTodo", "this and that");
        final ToDoNode toDoNode = new ToDoNode("sTodo", "bla");
        toDoNode.setNextNode(endNode);
        final PartNode partNode = new PartNode("bla", toDoNode, "ghostbusters");
        final EndNode end = new EndNode(nodeIdProvider.nextId());
        final RejectNode reject = new RejectNode("fReject","obvious.");
        start.setNextNode(new CallNode("fCall", partNode)).setNextNode(reject);
        DecisionGraph expected = new DecisionGraph();
        expected.add(start);
        expected.setStart(start);
        expected.addAllReachableNodes();
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");
        
        String code = "[>fTodo< todo: this and that][>fCall<call: ghostbusters][>fReject< reject: obvious.][-->ghostbusters< {title: bla} [>sTodo< todo: bla]--]";
        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());

        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }

    @Test
    public void considerTest() throws Exception {
        EndNode finalEndNode = new EndNode("2");
        ToDoNode todo1 = new ToDoNode("fTodo", "bla");
        ToDoNode todo2 = new ToDoNode("sTodo", "bla");
        PartNode pn1 = new PartNode("bla", todo1, "duh");
        PartNode pn2 = new PartNode("bla", todo2, "duh2");
        todo1.setNextNode(endNode);
        todo2.setNextNode(endNode);
        final CallNode whyNotCallNode = new CallNode("wnc", pn1);
        whyNotCallNode.setNextNode(finalEndNode);
        final CallNode whyNotCallNode2 = new CallNode("wnc2", pn2);
        whyNotCallNode2.setNextNode(finalEndNode);

        AtomicSlot t2Items = new AtomicSlot("Subject", "");
        AggregateSlot t2 = new AggregateSlot("Subject", "", t2Items);
        t2Items.registerValue("world", "");
        CompoundSlot ct = new CompoundSlot("topLevel", "");
        ct.addSubSlot(t2);
        CompoundValue tags = ct.createInstance();

        tags.put(t2.createInstance());
        ((AggregateValue) tags.get(t2)).add(t2Items.valueOf("world"));

        ConsiderNode start = new ConsiderNode("1", whyNotCallNode);
        start.setNodeFor(ConsiderOption.get(tags), whyNotCallNode2);
        DecisionGraph expected = new DecisionGraph();
        expected.add(whyNotCallNode);
        expected.add(start);

        expected.setStart(start);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");
        
        String code = "[>1< consider: \n"
                + "     {slot:Subject}\n"
                + "    {options:\n"
                + "        {world:[>wnc2< call: duh2]}\n"
                + "\n"
                + "    }\n"
                + "   { else: [>wnc< call: duh]}\n"
                + "]\n"
                + "[>2< end]"
                + "[-->duh< {title: bla} [>fTodo< todo: bla]--]"
                + "[-->duh2< {title: bla} [>sTodo< todo: bla]--]";

        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(ct, pmd, new ArrayList<>());
        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);
    }

    @Test
    public void askTest() throws Exception {

        AskNode start = new AskNode(nodeIdProvider.nextId());
        ToDoNode todoNode = new ToDoNode("secTodo", "bla");
        todoNode.setNextNode(endNode);
        EndNode end = new EndNode((nodeIdProvider.nextId()));
        PartNode callPart = new PartNode("bla", todoNode, "duh");
        start.setText("why?");
        start.addAnswer(Answer.get("dunno"), new EndNode("de"));
        final CallNode whyNotCallNode = new CallNode("wnc", callPart);
        whyNotCallNode.setNextNode(end);
        start.addAnswer(Answer.get("why not"), whyNotCallNode);

        DecisionGraph expected = new DecisionGraph();
        expected.add(start);
        expected.setStart(start);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");

        String code = "[ask: {text: why?} {answers: {dunno:[>de< end]} {why not:[>wnc< call: duh]}}][end][-->duh< {title: bla} [>secTodo< todo: bla]--]";
        
        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());

        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }

    @Test
    public void askWithImplicitAnswerTest() throws Exception {

        AskNode start = new AskNode(nodeIdProvider.nextId());
        start.setText("Should I?");
        start.addAnswer(Answer.YES, new EndNode("end-yes"));
        start.addAnswer(Answer.NO, new EndNode("end-no"));

        DecisionGraph expected = new DecisionGraph();
        expected.add(start);
        expected.setStart(start);

        String code = "[ask: {text: Should I?} {answers: {yes:[>end-yes< end]}}][>end-no< end]";
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(emptyTagSpace, endNode ,new ArrayList<>());
        
        DecisionGraph actual = cu.getDecisionGraph();

        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }
    
    @Test
    public void sectionTest() throws Exception {

        PartNode fisrt_partNode = new PartNode("part", "Part - start");
        ToDoNode callTodo = new ToDoNode("sTodo", "bla");
        PartNode partNode = new PartNode("bla", callTodo, "callid");
        ToDoNode sectionStartNode = new ToDoNode("blaID", "bla bla");
        CallNode call = new CallNode("CallID", partNode);
        sectionStartNode.setNextNode(call);
        fisrt_partNode.setStartNode(sectionStartNode);

        EndNode finalEndNode = new EndNode("[SYN-END]");
        call.setNextNode(finalEndNode);

        DecisionGraph expected = new DecisionGraph();
        expected.add(fisrt_partNode);
        expected.setStart(finalEndNode);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");
        String code = "[-->part< {title: Part - start} [>blaID< todo: bla bla] [>CallID< call: callid]--][-->callid< {title: bla} [>sTodo< todo: bla]--]";

        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());
        
        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }
    
    @Test
    public void callPartTest() throws Exception {
        PartNode partNode = new PartNode("part","");
        ToDoNode firstTodo = new ToDoNode("first_todo", "first todo");
        ToDoNode secondTodo = new ToDoNode("second_todo", "second todo");
        firstTodo.setNextNode(secondTodo);
        partNode.setStartNode(firstTodo);
        CallNode callNode = new CallNode("call", partNode);
        EndNode finalEndNode = new EndNode("[SYN-END]");
        callNode.setNextNode(finalEndNode);
        
        DecisionGraph expected = new DecisionGraph();
        expected.add(callNode);
        expected.setStart(callNode);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");

        String code = "[>call< call: part]\n" +
            "[-->part<\n" +
            "  [>first_todo< todo: first todo]\n" +
            "  [>second_todo< todo: second todo]\n" +
            "--]";

        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());
        
        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);
    }
    
    @Test
    public void noCallPartTest() throws Exception {
        PartNode partNode = new PartNode("part","");
        ToDoNode firstTodo = new ToDoNode("first_todo", "first todo");
        ToDoNode secondTodo = new ToDoNode("second_todo", "second todo");
        firstTodo.setNextNode(secondTodo);
        partNode.setStartNode(firstTodo);
        ToDoNode a_toDoNode = new ToDoNode("a", "a_todo");
        ToDoNode b_toDoNode = new ToDoNode("b", "b_todo");
        ToDoNode c_toDoNode = new ToDoNode("c", "c_todo");
        ToDoNode d_toDoNode = new ToDoNode("d", "d_todo");
        c_toDoNode.setNextNode(d_toDoNode);
        b_toDoNode.setNextNode(c_toDoNode);
        a_toDoNode.setNextNode(b_toDoNode);
        
        
        EndNode finalEndNode = new EndNode("[SYN-END]");
        d_toDoNode.setNextNode(finalEndNode);
        
        DecisionGraph expected = new DecisionGraph();
        expected.add(a_toDoNode);
        expected.setStart(a_toDoNode);
        expected.addPart(partNode);
        expected.prefixNodeIds("[.." + File.separator + "main.dg]");

        String code = "[>a< todo: a_todo]\n" +
                "[>b< todo: b_todo]\n" +
                "\n" +
                "[-->part<\n" +
                "  [>first_todo< todo: first todo]\n" +
                "  [>second_todo< todo: second todo]\n" +
                "--]\n" +
                "[>c< todo: c_todo]\n" +
                "[>d< todo: d_todo]";

        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/main.dg"));
        pmd.setMetadataFile(Paths.get("/test/main.dg"));
        pathToString.put(Paths.get("/main.dg"), code);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());
        
        normalize(actual);
        normalize(expected);

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);
    }
    

    @Test
    public void setEndTest() throws Exception {

        AtomicSlot t1 = new AtomicSlot("t1", "");
        t1.registerValue("a", "");
        t1.registerValue("b", "");
        t1.registerValue("c", "");

        AtomicSlot t2Items = new AtomicSlot("t2Items", "");
        AggregateSlot t2 = new AggregateSlot("t2", "", t2Items);
        t2Items.registerValue("b", "");
        t2Items.registerValue("c", "");

        CompoundSlot ct = new CompoundSlot("topLevel", "");
        ct.addSubSlot(t1);
        ct.addSubSlot(t2);

        CompoundValue tags = ct.createInstance();
        tags.put(t1.valueOf("a"));
        tags.put(t2.createInstance());
        ((AggregateValue) tags.get(t2)).add(t2Items.valueOf("b"));
        ((AggregateValue) tags.get(t2)).add(t2Items.valueOf("c"));

        SetNode start = new SetNode(nodeIdProvider.nextId(), tags);
        start.setNextNode(new EndNode(nodeIdProvider.nextId()));
        DecisionGraph expected = new DecisionGraph();
        expected.add(start);
        expected.setStart(start);
        expected.setId("loremIpsum");
        String code = "[set: t1=a; t2 += b,c][end]";
        
        
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(ct, endNode ,new ArrayList<>());
        
        DecisionGraph actual = cu.getDecisionGraph();
        actual.setId("loremIpsum"); // prevent a false negative over chart id.

        expected.nodes().forEach(n -> assertEquals(n, actual.getNode(n.getId())));
        actual.nodes().forEach(n -> assertEquals(n, expected.getNode(n.getId())));

        assertEquals(expected, actual);

    }

    @Test
    public void complexSetNodeTest() throws SyntaxErrorException, SemanticsErrorException, DataTagsParseException {
        String tsCode = "top: consists of mid1, mid2. "
                + "mid1: one of A, B, C, D. "
                + "mid2: consists of bottom1, bottom2. "
                + "bottom1: one of Q, W, E.\n"
                + "bottom2: some of A,S,D,F.";

        CompoundSlot ts = new TagSpaceParser().parse(tsCode).buildType("top").get();

        CompoundValue expected = ts.createInstance();
        expected.put(((AtomicSlot) ts.getSubSlot("mid1")).valueOf("B"));
        CompoundValue mid2 = ((CompoundSlot) ts.getSubSlot("mid2")).createInstance();
        mid2.put(((AtomicSlot) mid2.getSlot().getSubSlot("bottom1")).valueOf("W"));
        final AggregateValue bottom2Value = ((AggregateSlot) mid2.getSlot().getSubSlot("bottom2")).createInstance();
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("S"));
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("D"));
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("F"));
        mid2.put(bottom2Value);
        expected.put(mid2);

        String dgCode = "[set: mid1=B; bottom1=W; bottom2+=S,D,F][end]";
        
        CompilationUnit cu = new CompilationUnit(dgCode);
        cu.compile(ts, new EndNode("[SYN-END]"),new ArrayList<>());
        
        CompoundValue actual = ((SetNode) cu.getDecisionGraph().getStart()).getTags();

        assertEquals(expected, actual);
    }

    @Test
    public void dualSecondLevelNodeTest() throws SyntaxErrorException, SemanticsErrorException, DataTagsParseException {
        String tsCode = "top: consists of mid1, mid2. "
                + "mid1: one of A, B, C, D. "
                + "mid2: consists of bottom1, bottom2. "
                + "bottom1: one of Q, W, E.\n"
                + "bottom2: some of A,S,D,F.";

        CompoundSlot ts = new TagSpaceParser().parse(tsCode).buildType("top").get();

        CompoundValue mid2 = ((CompoundSlot) ts.getSubSlot("mid2")).createInstance();
        mid2.put(((AtomicSlot) mid2.getSlot().getSubSlot("bottom1")).valueOf("W"));
        final AggregateValue bottom2Value = ((AggregateSlot) mid2.getSlot().getSubSlot("bottom2")).createInstance();
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("S"));
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("D"));
        bottom2Value.add(bottom2Value.getSlot().getItemType().valueOf("F"));
        mid2.put(bottom2Value);

        CompoundValue expected = ts.createInstance();
        expected.put(mid2);

        String dgCode = "[set: bottom1=W; bottom2+=S,D,F][end]";
        
        CompilationUnit cu = new CompilationUnit(dgCode);
        cu.compile(ts, new EndNode("[SYN-END]"),new ArrayList<>());
        
        CompoundValue actual = ((SetNode) cu.getDecisionGraph().getStart()).getTags();

        assertEquals(expected, actual);
    }

    @Test
    public void multiLevelAtomicSetNodeTest() throws SyntaxErrorException, SemanticsErrorException, DataTagsParseException {
        String tsCode = "top: consists of mid1, mid2. "
                + "mid1: one of A, B, C, D. "
                + "mid2: consists of bottom1, bottom2. "
                + "bottom1: one of Q, W, E.\n"
                + "bottom2: some of A,S,D,F.";

        CompoundSlot ts = new TagSpaceParser().parse(tsCode).buildType("top").get();

        CompoundValue expected = ts.createInstance();
        expected.put(((AtomicSlot) ts.getSubSlot("mid1")).valueOf("B"));
        CompoundValue mid2 = ((CompoundSlot) ts.getSubSlot("mid2")).createInstance();
        mid2.put(((AtomicSlot) mid2.getSlot().getSubSlot("bottom1")).valueOf("W"));
        expected.put(mid2);

        String dgCode = "[set: mid1=B; bottom1=W][end]";
        
        CompilationUnit cu = new CompilationUnit(dgCode);
        cu.compile(ts, new EndNode("[SYN-END]"),new ArrayList<>());
        
        CompoundValue actual = ((SetNode) cu.getDecisionGraph().getStart()).getTags();

        assertEquals(expected, actual);
    }

    @Test
    public void singleSecondLevelAtomicSetNodeTest() throws SyntaxErrorException, SemanticsErrorException, DataTagsParseException {
        String tsCode = "top: consists of mid1, mid2. "
                + "mid1: one of A, B, C, D. "
                + "mid2: consists of bottom1, bottom2. "
                + "bottom1: one of Q, W, E.\n"
                + "bottom2: some of A,S,D,F.";

        CompoundSlot ts = new TagSpaceParser().parse(tsCode).buildType("top").get();

        CompoundValue expected = ts.createInstance();
        CompoundValue mid2 = ((CompoundSlot) ts.getSubSlot("mid2")).createInstance();
        mid2.put(((AtomicSlot) mid2.getSlot().getSubSlot("bottom1")).valueOf("W"));
        expected.put(mid2);

        String dgCode = "[set: bottom1=W][end]";
        CompilationUnit cu = new CompilationUnit(dgCode);
        cu.compile(ts, new EndNode("[SYN-END]"),new ArrayList<>());
        
        CompoundValue actual = ((SetNode) cu.getDecisionGraph().getStart()).getTags();

        assertEquals(expected, actual);
    }
    
    @Test
    public void importToTheMainFile() throws IOException {
        String code_a = "[#import b: b.dg]"
                + "[>nd-1< call: b>nd-2]"
                + "[-->nd-4<\n {title: bla} [>nd-5< todo: bla bla]--]";
        String code_b = "[#import a: a.dg]"
                + "[-->nd-2<\n {title: bla} [>nd-3< call: a>nd-4]--]";
        
        Map<Path, String> pathToString = new HashMap<>();
        PolicyModelData pmd = new PolicyModelData();
        pmd.setDecisionGraphPath(Paths.get("/a.dg"));
        pmd.setMetadataFile(Paths.get("/test/a.dg"));
        pathToString.put(Paths.get("/a.dg"), code_a);
        pathToString.put(Paths.get("/b.dg"), code_b);
        ContentReader contentReader = new MemoryContentReader(pathToString);
        DecisionGraphCompiler dgc = new DecisionGraphCompiler(contentReader);
        DecisionGraph actual = dgc.compile(emptyTagSpace, pmd, new ArrayList<>());
        normalize(actual);
        assertEquals( C.set("[.." + File.separator + "a.dg]nd-1",
                            "[.." + File.separator + "b.dg]nd-2", "[.." + File.separator + "b.dg]nd-3", 
                            "[.." + File.separator + "a.dg]nd-4", "[.." + File.separator + "a.dg]nd-5",
                            "[.." + File.separator + "b.dg][.." + File.separator + "a.dg][SYN-END]", 
                            "[.." + File.separator + "a.dg][SYN-END]"), actual.nodeIds());
    }
    
    private DecisionGraph normalize(DecisionGraph dg) {
        dg.setId("normalizedId");
        return dg;
    }
}
