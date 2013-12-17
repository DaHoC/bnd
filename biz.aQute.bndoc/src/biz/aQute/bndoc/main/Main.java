package biz.aQute.bndoc.main;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.lib.consoleapp.*;
import aQute.lib.env.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.glob.*;
import biz.aQute.bndoc.lib.*;

/**
 * This is the command line interface for bndoc.
 */
public class Main extends AbstractConsoleApp {

	public Main() throws Exception {
		super();
	}

	@Description("The main options for bndoc")
	interface BndocOptions extends MainOptions {}

	/**
	 * Local initialization point. Is called first for the global commands.
	 */

	public void __main(BndocOptions opts) throws IOException {
		super.__main(opts);
	}

	@Arguments(arg = {})
	@Description("Generate the output documents as specified in the local bndoc.bndoc properties file")
	interface GenerateOptions extends Options {

		@Description("Clean the output directories")
		boolean clean();

		@Description("Add additional properties.")
		List<String> properties();

		String output();

		List<String> shapes();
	}

	interface HtmlOptions extends GenerateOptions {
		String template();

		String inner();

		String resources();
		
		List<String> css();
	}

	static Pattern	WILDCARD_P	= Pattern.compile("([^*]*)(\\*)?\\*\\.(\\w+)$");

	@Description("Generate a single html file")
	public void _html(HtmlOptions options) throws Exception {
		DocumentBuilder db = new DocumentBuilder(this);

		File resources = getFile(options.resources() == null ? "www" : options.resources());
		resources.mkdirs();
		if ( !resources.isDirectory())
			error("No such directory (nor can it be made) %s", resources);
		
		File template = options.template() != null ? getFile(options.template(), "Template") : null;
		File inner = options.inner() != null ? getFile(options.inner(), "Inner template") : null;

		db.setResources(resources);
		
		System.out.println("Resources dir " + resources);

		if (template != null)
			db.setTemplate(IO.collect(template));
		else
			db.setTemplate(null);

		if (inner != null)
			db.setInner(IO.collect(inner));
		else
			db.setInner(null);

		if (options.output() != null) {
			db.setOutput(getFile(options.output()));

		} else {
			File tmp = File.createTempFile("bndoc", ".html");
			db.setOutput(tmp);
		}

		if ( options.css() != null)  {
			List<File> expand = expand(options.css());
			for ( File f : expand) {
				db.addCSS(f);
			}
		} 
		
		List<File> propertyFiles = expand(options.properties());
		for (File f : propertyFiles) {
			setProperties(f);
		}

		Header h = new Header(getProperty("symbols"));
		for (Entry<String,Props> entry : h.entrySet()) {
			db.addCustomShape(entry.getKey(), entry.getValue());
		}

		db.addSources(expand(options._()));

		db.prepare();

		if (isOk() && db.isOk()) {
			if (options.clean()) {
				IO.delete(resources);
				IO.delete(db.getOutput());
			}
			resources.mkdirs();
			
			db.single();

			if (options.output() == null)
				IO.copy(db.getOutput(), System.out);
		}
		getInfo(db);
	}

	private List<File> expand(List<String> list) {
		List<File> result = new ArrayList<>();
		if (list == null)
			return result;

		for (String source : list) {
			Matcher m = WILDCARD_P.matcher(source);
			boolean recursive = false;
			Glob glob = null;
			if (m.matches()) {
				source = m.group(1);
				if (source.isEmpty())
					source = ".";
				recursive = m.group(2) != null;
				glob = new Glob(m.group(3));
			}

			File f = getFile(source);
			if (f != null) {
				if (f.isDirectory()) {
					traverse(result, f, glob, recursive);
				} else if (f.isFile()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private void traverse(List<File> list, File f, Glob glob, boolean recursive) {
		for (File sub : f.listFiles()) {
			if (sub.isFile()) {
				if (glob == null || glob.matcher(sub.getName()).matches())
					list.add(sub);
			} else {
				if (recursive)
					traverse(list, sub, glob, recursive);
			}
		}

	}

	public static void main(String args[]) throws Exception {
		Main main = new Main();
		main.run(args);
	}
}