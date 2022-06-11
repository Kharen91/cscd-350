package cs350s22.component.ui.parser;

import java.io.*;
import java.util.*;

import cs350s22.component.A_Component;
import cs350s22.component.logger.LoggerMessage;
import cs350s22.component.logger.LoggerMessageSequencing;
import cs350s22.component.sensor.A_Sensor;
import cs350s22.component.sensor.mapper.A_Mapper;
import cs350s22.component.sensor.mapper.MapperEquation;
import cs350s22.component.sensor.mapper.MapperInterpolation;
import cs350s22.component.sensor.mapper.function.equation.EquationNormalized;
import cs350s22.component.sensor.mapper.function.equation.EquationPassthrough;
import cs350s22.component.sensor.mapper.function.equation.EquationScaled;
import cs350s22.component.sensor.mapper.function.interpolator.InterpolationMap;
import cs350s22.component.sensor.mapper.function.interpolator.InterpolatorLinear;
import cs350s22.component.sensor.mapper.function.interpolator.InterpolatorSpline;
import cs350s22.component.sensor.mapper.function.interpolator.loader.MapLoader;
import cs350s22.component.sensor.reporter.A_Reporter;
import cs350s22.component.sensor.reporter.ReporterChange;
import cs350s22.component.sensor.reporter.ReporterFrequency;
import cs350s22.component.sensor.watchdog.A_Watchdog;
import cs350s22.component.sensor.watchdog.WatchdogAcceleration;
import cs350s22.component.sensor.watchdog.WatchdogBand;
import cs350s22.component.sensor.watchdog.WatchdogHigh;
import cs350s22.component.sensor.watchdog.WatchdogLow;
import cs350s22.component.sensor.watchdog.WatchdogNotch;
import cs350s22.component.sensor.watchdog.mode.A_WatchdogMode;
import cs350s22.component.sensor.watchdog.mode.WatchdogModeAverage;
import cs350s22.component.sensor.watchdog.mode.WatchdogModeInstantaneous;
import cs350s22.component.sensor.watchdog.mode.WatchdogModeStandardDeviation;
import cs350s22.component.ui.CommandLineInterface;
import cs350s22.message.A_Message;
import cs350s22.message.actuator.MessageActuatorRequestPosition;
import cs350s22.message.ping.MessagePing;
import cs350s22.support.*;
import cs350s22.test.ActuatorPrototype;
import cs350s22.test.MySensor;

public class Parser{
	//Variable List
	public A_ParserHelper parserHelper;
	public String commandText;
	
	private String[] tokens;
	
	//CONSTRUCTORS
	public Parser(A_ParserHelper parserHelper, String commandText){
		this.parserHelper = parserHelper;
		this.commandText = commandText;
	}
	
	public void parse() throws ParseException, IOException{
		//Variable List
		String input = this.commandText;	// Raw input from user
		String parserCheck = null;			// First term of input
		String output = null;				// Input with first term removed
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    if(splitArray.length > 1) {
	    	output = splitArray[1];
	    }
	    
	    //Navigation Tree
		if(parserCheck.equals("CREATE")){		//If first term is CREATE
		    treeCreate(output);					//Send rest of input to treeCreate() branch
		}
		else if(parserCheck.equals("SEND")){	//If first term is SEND
		    treeSend(output);					//Send rest of input to treeSend() branch
		}
		
		//Immediate Action
		//CLOCK Methods
		if(parserCheck.equals("@CLOCK")){			//If first term is @CLOCK
		    Clock myClock = Clock.getInstance();    //Get a hold of the clock
			
			//Check if we're turning the clock on or off
			if(output.equals("PAUSE")){				//If second term is PAUSE
			    myClock.isActive(false);            //Turn the clock off
			}
			else if(output.equals("RESUME")){		//If second term is RESUME
			    myClock.isActive(true);             //Turn the clock on
			}
			
			//Check for anything else
			else{
			    String[] actionArray = output.split(" ", 2);	//Strip second term from input
				
				//Are we running "ONESTEP"
				if(actionArray[0].equals("ONESTEP")){					//If second term is ONESTEP
					int counter = Integer.parseInt(actionArray[1]);     //Convert third term VALUE to int
				    myClock.onestep(counter);							//Run appropriate method
				}
				
				//Are we running "SET RATE"
				else if(actionArray[0].equals("SET")){					//If second term is SET
					String[] setRate = actionArray[1].split(" ", 2);	//Split remaining string to isolate VALUE
					int counter = Integer.parseInt(setRate[1]);			//Convert VALUE to int
					myClock.setRate(counter);							//Run appropriate method
				}
			}
		}
		
		if(parserCheck.equals("@EXIT")){		//If first term is @EXIT
			this.parserHelper.exit();			//End the program
		}
		
		if(parserCheck.equals("@RUN")){			//If first term is @RUN
		    this.parserHelper.run(output);		//Run input command
		}
		if(parserCheck.equals("@CONFIGURE")){	//If first term is @CONFIGURE
			//CODE HERE							//Do we need to do anything? Is built-in @CONFIGURE good enough?
			//VARIABLE LIST
			Filespec file1 = null; //new Filespec("a.txt");
			Filespec file2 = null; //new Filespec("b.txt");
			Filespec file3 = null; //new Filespec("c.txt");
			
			//Split "output" into individual terms
			String[] configArray = output.split(" ", 0);
			
			//Iterate through configArray
			for(int i = 0; i < configArray.length; i++) {
				if(configArray[i].equals("LOG")) {
					file1 = new Filespec(configArray[i + 1]);
				}
				if(configArray[i].equals("SEQUENCE")) {
					file2 = new Filespec(configArray[i + 1]);
				}
				if(configArray[i].equals("NETWORK")) {
					file3 = new Filespec(configArray[i + 1]);
				}
			}
			
			LoggerMessage.initialize(file1);
			LoggerMessageSequencing.initialize(file2, file3);
			System.out.println("Log Files Initialized!");
		}
		if(parserCheck.equals("BUILD")){		//If first term is BUILD
			buildNetwork(output);				//Send remaining input to buildNetwork()
		}
	}

	public void treeCreate(String input) throws IOException {
		//VARIABLE LIST
		String parserCheck = null;	//Second term of raw input
		String output = null;		//Rest of user input
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    if(splitArray.length > 1) {		//Check to make sure that user implemented more than 2 terms
	    	output = splitArray[1];		//before assigning to "output", to prevent possible exceptions
	    }
	    
	    //Navigation Tree
	  	if(parserCheck.equals("ACTUATOR")){				//If user typed "CREATE ACTUATOR"...
	  		createActuator(output);						//Send remaining input to createActuator()
	  	}
	  	else if(parserCheck.equals("MAPPER")) {			//If user typed "CREATE MAPPER"...
	  		createMapper(output);						//Send remaining input to createMapper()
	  	}
	  	else if(parserCheck.equals("REPORTER")) {		//If user typed "CREATE REPORTER"...
	  		createReporter(output);						//Send remaining input to createReporter()
	  	}
	  	else if(parserCheck.equals("SENSOR")) {			//If user typed "CREATE SENSOR"...
	  		createSensor(output);						//Send remaining input to createSensor()
	  	}
	  	else if(parserCheck.equals("WATCHDOG")) {			//If user typed "CREATE WATCHDOG"
	  		//Insert Code Here
	  		String[] splitSub = output.split(" ", 2);		//Split output to determine what type
	  		
	  		if(splitSub[0] == "ACCELERATION"){				//"CREATE WATCHDOG ACCELERATION"...
	  			createWatchdogAcceleration(splitSub[1]);	//Send remaining input to createWatchdogAcceleration()
	  		}
	  		else if(splitSub[0] == "BAND"){					//"CREATE WATCHDOG BAND"...
	  			createWatchdogBand(splitSub[1]);			//Send remaining input to createWatchdogBand()
	  		}
	  		else if(splitSub[0] == "NOTCH"){				//"CREATE WATCHDOG NOTCH"...
	  			createWatchdogNotch(splitSub[1]);			//Send remaining input to createWatchdogNotch()
	  		}
	  		else if(splitSub[0] == "LOW"){					//"CREATE WATCHDOG LOW"...
	  			createWatchdogLow(splitSub[1]);				//Send remaining input to createWatchdogLow()
	  		}
	  		else if(splitSub[0] == "HIGH"){					//"CREATE WATCHDOG HIGH"...
	  			createWatchdogHigh(splitSub[1]);			//Send remaining input to createWatchdogHigh()
	  		}
	  	}
	}

	public void treeSend(String input) {	//Misnomer, tree wasn't actually necessary, we really just trim
		//VARIABLE LIST                     //input to prepare for actual send functions
		String parserCheck = null;
		String output = null;
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    //Prevent exceptions by checking for length before assigning "output"
	    if(splitArray.length > 1) {
	    	output = splitArray[1];
	    }
	    
	    //sendMessage(output);
	    
	}
	
//--------------------------------------------------------------------------------------
	
	public void createWatchdogAcceleration(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier watchdogName = Identifier.make(splitArray[0]);
		
		int average = 0;
		int standardDev = 0;
		
		double low;
		double high;
		
		int grace;
		
		for(int i = 1; i < splitArray.length; i ++)
		{
			if(splitArray[i] == "INSTANTANEOUS")
			{
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeInstantaneous();
					WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeInstantaneous();
				WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "AVERAGE")
			{
				if(splitArray[i+1] != "THRESHOLD")
				{
					average = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeAverage(average);
					WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeAverage(average);
				WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "STANDARD" && splitArray[i+1] == "DEVIATION")
			{
				i ++;
				
				if(splitArray[i+1] != "THRESHOLD")
				{
					standardDev = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
					WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
				WatchdogAcceleration w = new WatchdogAcceleration(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
		}
		
	}
	
	public void createWatchdogBand(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier watchdogName = Identifier.make(splitArray[0]);
		
		int average = 0;
		int standardDev = 0;
		
		double low;
		double high;
		
		int grace;
		
		for(int i = 1; i < splitArray.length; i ++)
		{
			if(splitArray[i] == "INSTANTANEOUS")
			{
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeInstantaneous();
					WatchdogBand w = new WatchdogBand(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeInstantaneous();
				WatchdogBand w = new WatchdogBand(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "AVERAGE")
			{
				if(splitArray[i+1] != "THRESHOLD")
				{
					average = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeAverage(average);
					WatchdogBand w = new WatchdogBand(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeAverage(average);
				WatchdogBand w = new WatchdogBand(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "STANDARD" && splitArray[i+1] == "DEVIATION")
			{
				i ++;
				if(splitArray[i+1] != "THRESHOLD")
				{
					standardDev = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
					WatchdogBand w = new WatchdogBand(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
				WatchdogBand w = new WatchdogBand(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
		}
	}
	
	public void createWatchdogNotch(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier watchdogName = Identifier.make(splitArray[0]);
		
		int average = 0;
		int standardDev = 0;
		
		double low;
		double high;
		
		int grace;
		
		for(int i = 1; i < splitArray.length; i ++)
		{
			if(splitArray[i] == "INSTANTANEOUS")
			{
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeInstantaneous();
					WatchdogNotch w = new WatchdogNotch(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeInstantaneous();
				WatchdogNotch w = new WatchdogNotch(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "AVERAGE")
			{
				if(splitArray[i+1] != "THRESHOLD")
				{
					average = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeAverage(average);
					WatchdogNotch w = new WatchdogNotch(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeAverage(average);
				WatchdogNotch w = new WatchdogNotch(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			else if(splitArray[i] == "STANDARD" && splitArray[i+1] == "DEVIATION")
			{
				i ++;
				if(splitArray[i+1] != "THRESHOLD")
				{
					standardDev = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				i ++;
				i ++;
				low = Double.parseDouble(splitArray[i]);
				i ++;
				high = Double.parseDouble(splitArray[i]);
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
					WatchdogNotch w = new WatchdogNotch(low, high, mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
				WatchdogNotch w = new WatchdogNotch(low, high, mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
		}
	}
	
	public void createWatchdogLow(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier watchdogName = Identifier.make(splitArray[0]);
		
		int average = 0;
		int standardDev = 0;
		int threshold = 0;
		
		int grace;
		
		for(int i = 1; i < splitArray.length; i ++)
		{
			if(splitArray[i] == "INSTANTANEOUS")
			{
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeInstantaneous();
					WatchdogLow w = new WatchdogLow(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeInstantaneous();
				WatchdogLow w = new WatchdogLow(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			if(splitArray[i] == "AVERAGE")
			{
				if(splitArray[i + 1] != "THRESHOLD")
				{
					average = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeAverage(average);
					WatchdogLow w = new WatchdogLow(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeAverage(average);
				WatchdogLow w = new WatchdogLow(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			if(splitArray[i] == "STANDARD" && splitArray[i+1] == "DEVIATION")
			{
				i ++;
				if(splitArray[i + 1] != "THRESHOLD")
				{
					standardDev = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
					WatchdogLow w = new WatchdogLow(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
				WatchdogLow w = new WatchdogLow(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
		}
	}
	
	public void createWatchdogHigh(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier watchdogName = Identifier.make(splitArray[0]);
		
		int average = 0;
		int standardDev = 0;
		int threshold = 0;
		
		int grace;
		
		for(int i = 1; i < splitArray.length; i ++)
		{
			if(splitArray[i] == "INSTANTANEOUS")
			{
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeInstantaneous();
					WatchdogHigh w = new WatchdogHigh(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeInstantaneous();
				WatchdogHigh w = new WatchdogHigh(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			if(splitArray[i] == "AVERAGE")
			{
				if(splitArray[i + 1] != "THRESHOLD")
				{
					average = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeAverage(average);
					WatchdogHigh w = new WatchdogHigh(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeAverage(average);
				WatchdogHigh w = new WatchdogHigh(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
			if(splitArray[i] == "STANDARD" && splitArray[i+1] == "DEVIATION")
			{
				i ++;
				if(splitArray[i + 1] != "THRESHOLD")
				{
					standardDev = Integer.parseInt(splitArray[i + 1]);
					i ++;
				}
				
				i ++;
				threshold = Integer.parseInt(splitArray[i + 1]);
				
				if(splitArray[i + 1] == "GRACE")
				{
					grace = Integer.parseInt(splitArray[i + 2]);
					
					A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
					WatchdogHigh w = new WatchdogHigh(threshold,mode, grace);
					parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
				}
				
				A_WatchdogMode mode = new WatchdogModeStandardDeviation(standardDev);
				WatchdogHigh w = new WatchdogHigh(threshold,mode);
				parserHelper.getSymbolTableWatchdog().add(watchdogName, w);
			}
		}
	}
	
//--------------------------------------------------------------------------------------
	
	public void createReporter(String input)
	{
		String[] splitArray = input.split(" ", 0);
		Identifier reporterName = Identifier.make(splitArray[0]);
		int delta = 0;
		int frequency = 0;
		
		List<Identifier> reporterIDs = null;
		List<Identifier> reporterGroups = null;
		int reporterIDSlot = 0;
		int reporterGroupSlot = 0;
    	
		
		if(splitArray[0] == "FREQUENCY")
		{
	    	for(int scan = 3; scan < splitArray.length; scan++) {
	    		if(splitArray[scan].equals("IDS")) {
	    			reporterIDSlot = scan;
	    		}
	    		else if(splitArray[scan].equals("GROUPS")) {
	    			reporterGroupSlot = scan;
	    		}
	    	}
			
			for(int i = 3; i < splitArray.length; i++) {
	    		if(splitArray[i].equals("IDS")) {
	    			
	    			if(reporterIDSlot != 0) {
	    				for(int j = i+1; j < reporterIDSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					reporterIDs.add(val);
	    					
	    				}
	    			}
	    		}
	    		else if(splitArray[i].equals("GROUPS"))
	    		{
	    			if(reporterGroupSlot != 0) {
	    				for(int j = i+1; j < reporterGroupSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					reporterGroups.add(val);
	    					
	    				}
	    			}
	    		}
	    		if(splitArray[i].equals("FREQUENCY")) {	//Once we find the "FREQUENCY" term
		   			
	    			frequency = Integer.parseInt(splitArray[i+1]);
					
	    		}
			}
			
			ReporterFrequency r = new ReporterFrequency(reporterIDs, reporterGroups, frequency);
			parserHelper.getSymbolTableReporter().add(reporterName, r);
		}
		
		else if(splitArray[0] == "CHANGE")
		{
			for(int scan = 3; scan < splitArray.length; scan++) {
	    		if(splitArray[scan].equals("IDS")) {
	    			reporterIDSlot = scan;
	    		}
	    		else if(splitArray[scan].equals("GROUPS")) {
	    			reporterGroupSlot = scan;
	    		}
	    	}
			
			for(int i = 3; i < splitArray.length; i++) {
				if(splitArray[i].equals("IDS")) {
	    			
	    			if(reporterIDSlot != 0) {
	    				for(int j = i+1; j < reporterIDSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					reporterIDs.add(val);
	    					
	    				}
	    			}
	    		}
	    		else if(splitArray[i].equals("GROUPS"))
	    		{
	    			if(reporterGroupSlot != 0) {
	    				for(int j = i+1; j < reporterGroupSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					reporterGroups.add(val);
	    					
	    				}
	    			}
	    		}
	    		if(splitArray[i].equals("DELTA")) {	//Once we find the "DELTA" term
		   			
	    			delta = Integer.parseInt(splitArray[i+1]);
					
	    		}
			}
			
			ReporterChange r = new ReporterChange(reporterIDs, reporterGroups, delta);
			parserHelper.getSymbolTableReporter().add(reporterName, r);
		}
	}

//--------------------------------------------------------------------------------------
	
	public void createSensor(String input){
		//Split off leading term into first slot of array
		String[] splitArray = input.split(" ", 0);
		
	    String sensorType = splitArray[0];						//The first term is always the type
	    Identifier sensorName = Identifier.make(splitArray[1]);	//The second term is always the name
	    
	    //If the resulting array is only two terms long
	    if(splitArray.length == 2){
	    	MySensor s = new MySensor(sensorName);
	    	System.out.println("Sensor Created");
	    	parserHelper.getSymbolTableSensor().add(sensorName, s);
	    	System.out.println("Sensor Added to Table");
	    }
	    
	    
	    
	    else{
	    	Identifier myGroup = null;
	    	List<Identifier> sensorGroups= null;
	    	
	    	List<A_Reporter> sensorReporters = null;
	    	List<A_Watchdog> sensorWatchdogs = null;
	    	A_Mapper sensorMapper = null;
	    	
	    	int reporterSlot = 0;
	    	int watchdogSlot = 0;
	    	int mapperSlot = 0;
	    	
	    	for(int scan = 3; scan < splitArray.length; scan++) {
	    		if(splitArray[scan].equals("REPORTERS")) {
	    			reporterSlot = scan;
	    		}
	    		if(splitArray[scan].equals("WATCHDOGS")) {
	    			watchdogSlot = scan;
	    		}
	    		if(splitArray[scan].equals("MAPPER")) {
	    			mapperSlot = scan;
	    		}
	    	}
	    	
	    	for(int i = 3; i < splitArray.length; i++) {
	    		if(splitArray[i].equals("GROUP")){		//Once we find the "GROUP" term
	    			
	    			myGroup = Identifier.make(splitArray[i+1]);
	    			sensorGroups.add(myGroup);
	    			
	    			
	    		}
	    		//parserHelper.getSymbolTableSensor().add(sensorName, s);
	    		//parserHelper.getSymbolTableReporter().get(variableName);
	    		if(splitArray[i].equals("REPORTERS")) {
	    			//NOTE COULD BE REPORTER OR REPORTERS
	    			if(watchdogSlot != 0) {
	    				for(int j = i+1; j < watchdogSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					
	    					A_Reporter thisReporter = parserHelper.getSymbolTableReporter().get(val);
	    					sensorReporters.add(thisReporter);
	    					
	    				}
	    			}
	    			else if (mapperSlot != 0) {
	    				for(int j = i+1; j < mapperSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					
	    					A_Reporter thisReporter = parserHelper.getSymbolTableReporter().get(val);
	    					sensorReporters.add(thisReporter);
	    				}
	    			}
	    			else {
	    				for(int j = i+1; j < splitArray.length; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					
	    					A_Reporter thisReporter = parserHelper.getSymbolTableReporter().get(val);
	    					sensorReporters.add(thisReporter);
	    				}
	    			}
	    		}
	    		if(splitArray[i].equals("WATCHDOGS")) {
	    			//NOTE COULD BE WATCHDOGS OR WATCHDOG
	    			if (mapperSlot != 0) {
	    				for(int j = i+1; j < mapperSlot; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					
	    					A_Watchdog thisWatchdog = parserHelper.getSymbolTableWatchdog().get(val);
	    					sensorWatchdogs.add(thisWatchdog);
	    				}
	    			}
	    			else {
	    				for(int j = i+1; j < splitArray.length; j++) {
	    					Identifier val = Identifier.make(splitArray[j]);
	    					
	    					A_Watchdog thisWatchdog = parserHelper.getSymbolTableWatchdog().get(val);
	    					sensorWatchdogs.add(thisWatchdog);
	    				}
	    			}
	    		}
	    		if(splitArray[i].equals("MAPPER")) {	//Once we find the "MAPPER" term
	    			   			
	    			Identifier val = Identifier.make(splitArray[i + 1]);
					sensorMapper = parserHelper.getSymbolTableMapper().get(val);
	    		}
	    	}
	    	
	    	//build final constructor here
	    	//sensor(sensorName, sensorGroups, sensorReporters, sensorWatchdogs, sensorMapper)
	    	if(sensorMapper != null) {
	    		//There is a Mapper
	    		MySensor s = new MySensor(sensorName, sensorGroups, sensorReporters, sensorWatchdogs, sensorMapper);
		    	System.out.println("Sensor s3 Created");
		    	parserHelper.getSymbolTableSensor().add(sensorName, s);
		    	System.out.println("Sensor s3 Added to Table");
	    	}
	    	else {
	    		//There is no Mapper
	    		MySensor s = new MySensor(sensorName, sensorGroups, sensorReporters, sensorWatchdogs);
		    	System.out.println("Sensor s2 Created");
		    	parserHelper.getSymbolTableSensor().add(sensorName, s);
		    	System.out.println("Sensor s2 Added to Table");
	    	}
	    }
	}//end createSensor

//--------------------------------------------------------------------------------------
	
	public void createActuator(String input) {
		//Variable List
		String myName = null;
		Identifier myGroup = null;
    	List<Identifier> actuatorGroups= Collections.<Identifier>emptyList();
		
    	A_Sensor mySensor = null;
    	List<A_Sensor> sensorGroups= Collections.<A_Sensor>emptyList();
    	
		double accelLeadin = 0.0;
		double accelLeadout = 0.0;
		double accelRelax = 0.0;
		double initValue = 0.0;
		double velocityLimit = 0.0;
		double minValue = 0.0;
		double maxValue = 0.0;
		double jerkLimit = 0.0;
		
		//Split up input into an array we can traverse
		String[] splitArray = input.split(" ", 0);
		
		//Pull out locations that we know for sure
		myName = splitArray[1];
		Identifier actuatorName = Identifier.make(myName);
		
		
		//Variables for Labels
		int sensorSlot = 0;
		int accelSlot = 0;
		
		//Get label locations for easier traversal
		//Also, get simple terms extracted when possible
		for(int scan = 0; scan < splitArray.length; scan++) {
			if(splitArray[scan].equals("SENSOR") || splitArray[scan].equals("SENSORS")) {
				sensorSlot = scan;
			}
			if(splitArray[scan].equals("ACCELERATION")) {
				accelSlot = scan;
			}
			if(splitArray[scan].equals("LEADIN")) {
				accelLeadin = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("LEADOUT")) {
				accelLeadout = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("RELAX")) {
				accelRelax = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("VELOCITY")) {
				velocityLimit = Double.parseDouble(splitArray[scan + 2]);
			}
			if(splitArray[scan].equals("MIN")) {
				minValue = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("MAX")) {
				maxValue = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("INITIAL")) {
				initValue = Double.parseDouble(splitArray[scan + 1]);
			}
			if(splitArray[scan].equals("JERK")) {
				jerkLimit = Double.parseDouble(splitArray[scan + 2]);
			}
		}
		
		//Get Group List + Sensor List
		if(sensorSlot != 0) //fix this logic!
		{
			for(int i = 2; i < sensorSlot; i++) {
				myGroup = Identifier.make(splitArray[i]);
				actuatorGroups.add(myGroup);
			}
			for(int i = sensorSlot + 1; i < accelSlot; i++) {
				Identifier val = Identifier.make(splitArray[i]);
				
				mySensor = parserHelper.getSymbolTableSensor().get(val);
				sensorGroups.add(mySensor);
			}
		}
		
		ActuatorPrototype a = new ActuatorPrototype(actuatorName, actuatorGroups, accelLeadin, accelLeadout, accelRelax, velocityLimit, initValue, minValue, maxValue, jerkLimit, sensorGroups);
		System.out.println("Actuator Created");
    	parserHelper.getSymbolTableActuator().add(actuatorName, a);
    	System.out.println("Actuator Added to Table");
	}//end createActuator
	
//--------------------------------------------------------------------------------------
	
	public void createMapper(String input) throws IOException{		//This should cover C1 - C4 all at once!
		//Split input into searchable array
		String[] splitArray = input.split(" ", 0);
		
		//The first term will always be the name
		Identifier mapperName = Identifier.make(splitArray[0]);
		
		//The first substantial difference is with term 3
		if(splitArray[2].equals("PASSTHROUGH")) {
			//We know everything we need and can start on the end-result!
			EquationPassthrough math = new EquationPassthrough();
			MapperEquation newMapper = new MapperEquation(math);
			parserHelper.getSymbolTableMapper().add(mapperName, newMapper);
		}
		if(splitArray[2].equals("SCALE")) {
			//We know everything we need, extract scale value and start on the end result!
			double scaleValue = Double.parseDouble(splitArray[3]);
			EquationScaled math = new EquationScaled(scaleValue);
			MapperEquation newMapper = new MapperEquation(math);
			parserHelper.getSymbolTableMapper().add(mapperName, newMapper);
		}
		if(splitArray[2].equals("NORMALIZE")) {
			//We know everything we need, extract two values and start on the end result!
			double minValue = Double.parseDouble(splitArray[3]);
			double maxValue = Double.parseDouble(splitArray[4]);
			EquationNormalized math = new EquationNormalized(minValue, maxValue);
			MapperEquation newMapper = new MapperEquation(math);
			parserHelper.getSymbolTableMapper().add(mapperName, newMapper);
		}
		if(splitArray[2].equals("LINEAR") || splitArray[2].equals("SPLINE")) {
			//Extract file name,create "Filespec" from it, and from there, the mapLoader
			Filespec myFile = new Filespec(splitArray[4]);
			MapLoader myLoader = new MapLoader(myFile);
			InterpolationMap myMap = myLoader.load();
			
			//Get correct interpolator
			if(splitArray[2].equals("LINEAR")) {
				InterpolatorLinear myReader = new InterpolatorLinear(myMap);
				MapperInterpolation newMapper = new MapperInterpolation(myReader);
				parserHelper.getSymbolTableMapper().add(mapperName, newMapper);
			}
			if(splitArray[2].equals("SPLINE")) {
				InterpolatorSpline myReader = new InterpolatorSpline(myMap);
				MapperInterpolation newMapper = new MapperInterpolation(myReader);
				parserHelper.getSymbolTableMapper().add(mapperName, newMapper);
			}
		}
	}//end createMapper
	
//--------------------------------------------------------------------------------------
	
	public void sendMessage(Scanner sc)
    {
	 	ArrayList<Identifier> id = new ArrayList<Identifier>();
		ArrayList<Identifier> group = new ArrayList<Identifier>();
        ArrayList<Identifier> currList = new ArrayList<Identifier>();
    	
        
		System.out.println("SENDING MESSAGE...");
        
        CommandLineInterface cli = parserHelper.getCommandLineInterface();
        
        if(tokens[2].matches("PING")) {
        	
        	System.out.println("SENDING PING");
        	
        	MessagePing ping = new MessagePing();
        	cli.issueMessage(ping);
        	parserHelper.getCommandLineInterface().issueMessage(ping);
        	
        }
        else if(tokens[tokens.length-2].matches("REQUEST")) {


		String curr = "";
		

		while(sc.hasNext()) {
			curr = sc.next(); 

			if(curr.equals("ID") || curr.equals("IDS")) {
				currList = id;
				
			}
			 
			else if(curr.equals("GROUP") || curr.equals("GROUPS")) {
				currList = group; 
				
			}

			else if(curr.equals("POSITION")) {
				break; 
				
			}

			else {
				
				currList.add(Identifier.make(curr)); 
				
			}
		 }
        }
        else if(tokens[tokens.length-1].matches("REPORT")) {

		boolean isRequest = (sc.next().equals("REQUEST"));
		double value = 0; 
		

		if(sc.hasNextDouble()) {
			value = sc.nextDouble();
		}
		ArrayList<Identifier> listOutput; 
		listOutput = (id.size() > 0) ? id : group; 

		
		if(!isRequest) {
			if(id.size() > 0) {
			//get message from actuator	
				cli.issueMessage(new MessageActuatorRequestPosition(id, value)); 
				
			}
			if(group.size() > 0) {
				A_Message message = new MessageActuatorRequestPosition(id, value);
				cli.issueMessage(message);
			}
			
		}
		else {
			if(id.size() > 0) {

				A_Message message = new MessageActuatorRequestPosition(id, value);
				cli.issueMessage(message);
			}
			if(group.size() > 0) {

				A_Message message = new MessageActuatorRequestPosition(group, value, 0);
				cli.issueMessage(message);
			}
			
			
		}
      }
    }
	
//--------------------------------------------------------------------------------------
	
	public void buildNetwork(String input) {
		//Split input string as usual
		String[] splitArray = input.split(" ", 0);
		
		//Variable List
		Identifier myName = null; 		//Identifier to store the name and bring up appropriate components
		A_Component newPiece = null;	//Variable to store components that are called, so we can tie them into the network
		
		//Iterate through input array
		for(int i = 0; i < splitArray.length; i++) {
			if(splitArray[i].equals("COMPONENT") || splitArray[i].equals("COMPONENTS")) {
				for(int j = i+1; j < splitArray.length; j++) {
					//Make an Identifier out of the term
					myName = Identifier.make(splitArray[j]);

					//Figure out what type of piece is needed, then get it
					if(parserHelper.getSymbolTableController().contains(myName) == true){
						newPiece = parserHelper.getSymbolTableController().get(myName);
					}
					else if(parserHelper.getSymbolTableActuator().contains(myName) == true){
						newPiece = parserHelper.getSymbolTableActuator().get(myName);
					}

					else if(parserHelper.getSymbolTableSensor().contains(myName) == true){
						newPiece = parserHelper.getSymbolTableSensor().get(myName);
					}
					//Add to ControllerMaster
					parserHelper.getControllerMaster().addComponent(newPiece);
				}
			}
		}
		
		parserHelper.getNetwork().writeOutput();
		
	}
	
}//end class