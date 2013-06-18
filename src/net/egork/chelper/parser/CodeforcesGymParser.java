package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesGymParser implements Parser {
	public Icon getIcon() {
		return IconLoader.getIcon("/icons/codeforces.png");
	}

	public String getName() {
		return "Codeforces Gym";
	}

    public void getContests(DescriptionReceiver receiver) {
        String contestsPage = FileUtilities.getWebPageContent("http://codeforces.ru/gyms");
		if (contestsPage == null)
			return;
        List<Description> contests = new ArrayList<Description>();
        StringParser parser = new StringParser(contestsPage);
        try {
            parser.advance(true, "<div class=\"contestList\">");
            parser.advance(true, "</tr>");
            while (parser.advanceIfPossible(true, "data-contestId=\"") != null) {
                String id = parser.advance(false, "\"");
                parser.advance(true, "<td>");
                String name = parser.advance(false, "</td>", "<br/>").trim();
                contests.add(new Description(id, name));
            }
        } catch (ParseException ignored) {}
		if (!receiver.isStopped())
			receiver.receiveDescriptions(contests);
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        String mainPage = FileUtilities.getWebPageContent("http://codeforces.com/gym/" + id);
		if (mainPage == null)
			return;
        List<Description> ids = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        try {
            parser.advance(true, "<table class=\"problems\">");
            while (parser.advanceIfPossible(true, "<a href=\"/gym/" + id + "/problem/") != null) {
                String taskID = parser.advance(false, "\">");
                parser.advance(true, "<a href=\"/gym/" + id + "/problem/" + taskID + "\">");
                String name = taskID + " - " + parser.advance(false, "</a>");
                ids.add(new Description(id + " " + taskID, name));
            }
        } catch (ParseException ignored) {
        }
        if (!receiver.isStopped())
			receiver.receiveDescriptions(ids);
    }

    public Task parseTask(Description description) {
		String id = description.id;
        String[] tokens = id.split(" ");
        if (tokens.length != 2)
            return null;
        String contestId = tokens[0];
        id = tokens[1];
        String text = FileUtilities.getWebPageContent("http://codeforces.com/gym/" + contestId + "/problem/" + id);
		if (text == null)
			return null;
        StringParser parser = new StringParser(text);
        try {
            parser.advance(false, "<div class=\"memory-limit\">", "<DIV class=\"memory-limit\">");
            parser.advance(true, "</div>", "</DIV>");
            String heapMemory = parser.advance(false, "</div>", "</DIV>").split(" ")[0] + "M";
            parser.advance(false, "<div class=\"input-file\">", "<DIV class=\"input-file\">");
            parser.advance(true, "</div>", "</DIV>");
            String inputFileName = parser.advance(false, "</div>", "</DIV>");
            StreamConfiguration inputType;
            if ("standard input".equals(inputFileName))
                inputType = StreamConfiguration.STANDARD;
            else
                inputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, inputFileName);
            parser.advance(false, "<div class=\"output-file\">", "<DIV class=\"output-file\">");
            parser.advance(true, "</div>", "</DIV>");
            String outputFileName = parser.advance(false, "</div>", "</DIV>");
            StreamConfiguration outputType;
            if ("standard output".equals(outputFileName))
                outputType = StreamConfiguration.STANDARD;
            else
                outputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, outputFileName);
            List<Test> tests = new ArrayList<Test>();
            while (true) {
                try {
                    parser.advance(false, "<div class=\"input\">", "<DIV class=\"input\">");
                    parser.advance(true, "<pre>", "<PRE>");
                    String testInput = parser.advance(false, "</pre>", "</PRE>").replace("<br />", "\n").replace("<BR/>", "\n");
                    parser.advance(false, "<div class=\"output\">", "<DIV class=\"output\">");
                    parser.advance(true, "<pre>", "<PRE>");
                    String testOutput = parser.advance(false, "</pre>", "</PRE>").replace("<br />", "\n").replace("<BR/>", "\n");
                    tests.add(new Test(StringEscapeUtils.unescapeHtml(testInput),
                            StringEscapeUtils.unescapeHtml(testOutput), tests.size()));
                } catch (ParseException e) {
                    break;
                }
            }
            String taskClass = "Task" + id;
			String name = description.description;
            return new Task(name, null, inputType, outputType, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + heapMemory, "Main", taskClass, TokenChecker.class.getCanonicalName(), "", new String[0], null,
                    null, true, null, null, false, false);
        } catch (ParseException e) {
            return null;
        }
    }

	public TestType defaultTestType() {
		return TestType.SINGLE;
	}
}