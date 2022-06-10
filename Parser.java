package cs350s22.component.ui.parser;

import java.io.*;
import java.util.*;

import cs350s22.test.*;
import cs350s22.component.*;
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
import cs350s22.component.sensor.watchdog.A_Watchdog;
import cs350s22.support.*;

public class Parser{
	//Variable List
	public A_ParserHelper parserHelper;
	public String commandText;
	
	//CONSTRUCTORS
	public Parser(A_ParserHelper parserHelper, String commandText){
		this.parserHelper = parserHelper;
		this.commandText = commandText;
	}
	
	public void parse() throws ParseException, IOException{
		String input = this.commandText;
		String parserCheck = null;
		String output = null;
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    if(splitArray.length > 1) {
	    	output = splitArray[1];
	    }
	    
	    //Navigation Tree
		if(parserCheck.equals("CREATE")){
		    treeCreate(output);
		}
		else if(parserCheck.equals("SEND")){
		    treeSend(output);
		}
		
		//Immediate Action
		//CLOCK Methods
		if(parserCheck.equals("@CLOCK")){
		    Clock myClock = Clock.getInstance();    //Get a hold of the clock
			
			//Check if we're turning the clock on or off
			if(output.equals("PAUSE")){
			    myClock.isActive(false);            //Turn the clock off
			}
			else if(output.equals("RESUME")){
			    myClock.isActive(true);             //Turn the clock on
			}
			
			//Check for anything else
			else{
			    String[] actionArray = output.split(" ", 2);
				
				//Are we running "ONESTEP"
				if(actionArray[0].equals("ONESTEP")){
					int counter = Integer.parseInt(actionArray[1]);     //Convert VALUE to int
				    myClock.onestep(counter);							//Run appropriate method
				}
				
				//Are we running "SET RATE"
				else if(actionArray[0].equals("SET")){
					String[] setRate = actionArray[1].split(" ", 2);	//Split remaining string to isolate VALUE
					int counter = Integer.parseInt(setRate[1]);			//Convert VALUE to int
					myClock.setRate(counter);							//Run appropriate method
				}
			}
		}
		
		if(parserCheck.equals("@EXIT")){
			this.parserHelper.exit();
		}
		
		if(parserCheck.equals("@RUN")){
		    this.parserHelper.run(output);
		}
		if(parserCheck.equals("@CONFIGURE")){
			//Insert Code Here
		}
		if(parserCheck.equals("BUILD")){
			buildNetwork(output);
		}
	}

	public void treeCreate(String input) throws IOException {
		String parserCheck = null;
		String output = null;
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    if(splitArray.length > 1) {
	    	output = splitArray[1];
	    }
	    
	    //Navigation Tree
	  	if(parserCheck.equals("ACTUATOR")){
	  	    createActuator(output);
	  	}
	  	else if(parserCheck.equals("CONTROLLER")) {
	  		//Insert Code Here
	  	}
	  	else if(parserCheck.equals("DEPENDENCY")) {
	  		//Insert Code Here
	  	}
	  	else if(parserCheck.equals("MAPPER")) {
	  		createMapper(output);
	  	}
	  	else if(parserCheck.equals("REPORTER")) {
	  		//Insert Code Here
	  	}
	  	else if(parserCheck.equals("SENSOR")) {
	  		createSensor(output);
	  	}
	  	else if(parserCheck.equals("WATCHDOG")) {
	  		//Insert Code Here
	  	}
	}
	
	public void treeSend(String input) {
		String parserCheck = null;
		String output = null;
		
		//Split off leading term into 2-slot array
		String[] splitArray = input.split(" ", 2);
	    parserCheck = splitArray[0];
	    
	    if(splitArray.length > 1) {
	    	output = splitArray[1];
	    }
	    
	    //Navigation Tree
	    
	}
	
	public void createActuator(String input) {
		//Variable List
		String myName = null;
		Identifier myGroup = null;
    	List<Identifier> actuatorGroups= null;
		
    	A_Sensor mySensor = null;
    	List<A_Sensor> sensorGroups= null;
    	
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
		for(int i = 2; i < sensorSlot; i++) {
			myGroup = Identifier.make(splitArray[i]);
			actuatorGroups.add(myGroup);
		}
		for(int i = sensorSlot + 1; i < accelSlot; i++) {
			Identifier val = Identifier.make(splitArray[i]);
			
			mySensor = parserHelper.getSymbolTableSensor().get(val);
			sensorGroups.add(mySensor);
		}
		
		ActuatorPrototype a = new ActuatorPrototype(actuatorName, actuatorGroups, accelLeadin, accelLeadout, accelRelax, velocityLimit, initValue, minValue, maxValue, jerkLimit, sensorGroups);
		System.out.println("Actuator Created");
    	parserHelper.getSymbolTableActuator().add(actuatorName, a);
    	System.out.println("Actuator Added to Table");
	}
	
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
	}
	
	//id   EQUATION       PASSTHROUGH
	//id   EQUATION       SCALE              value
	//id   EQUATION       NORMALIZE          value1       value2
	//id   INTERPOLATION  (LINEAR | SPLINE)  DEFINITION   string
	
	public void createSensor(String input){
		
		String output = null;
		
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
	}
	
	public void buildNetwork(String input) {
		//Split input string as usual
		String[] splitArray = input.split(" ", 0);
		
		//Variable List
		Identifier myName = null; 		//Identifier.make("input");
		A_Component newPiece = null;
		
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
}