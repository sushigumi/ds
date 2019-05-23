package main.java.unimelb.bitbox;

import org.kohsuke.args4j.Option;

//import main.java.unimelb.bitbox.util.HostPort; ?

//This class is where the arguments read from the command line will be stored
//Declare one field for each argument and use the @Option annotation to link the field
//to the argument name, args4J will parse the arguments and based on the name,  
//it will automatically update the field with the parsed argument value
public class CmdLineArgs {

	@Option(required = true, name = "-c", aliases = {"-client"}, usage = "Command name")
	private String commandname;
	
	@Option(required = true, name = "-s", usage = "HostPort")
	private String serverhostport;

	@Option(required = false, name = "-p", usage = "HostPort")
	private String peerhostport;
	
	public String getCommandName() {
		return commandname;
	}
	
	public String getServerHostPort() {
		return serverhostport;
	}

	public String getPeerHostPort() {
		return peerhostport;
	}
	
}

