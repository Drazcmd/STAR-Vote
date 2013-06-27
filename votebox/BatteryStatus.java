/**
  * This file is part of VoteBox.
  * 
  * VoteBox is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 3 as published by
  * the Free Software Foundation.
  * 
  * You should have received a copy of the GNU General Public License
  * along with VoteBox, found in the root of any distribution or
  * repository containing all or part of VoteBox.
  * 
  * THIS SOFTWARE IS PROVIDED BY WILLIAM MARSH RICE UNIVERSITY, HOUSTON,
  * TX AND IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS, IMPLIED OR
  * STATUTORY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
  * ACCURACY, COMPLETENESS, AND NONINFRINGEMENT.  THE SOFTWARE USER SHALL
  * INDEMNIFY, DEFEND AND HOLD HARMLESS RICE UNIVERSITY AND ITS FACULTY,
  * STAFF AND STUDENTS FROM ANY AND ALL CLAIMS, ACTIONS, DAMAGES, LOSSES,
  * LIABILITIES, COSTS AND EXPENSES, INCLUDING ATTORNEYS' FEES AND COURT
  * COSTS, DIRECTLY OR INDIRECTLY ARISING OUR OF OR IN CONNECTION WITH
  * ACCESS OR USE OF THE SOFTWARE.
 */

package votebox;

import sexpression.stream.Base64;

import java.io.*;

/**
 * @author Montrose, Matt Bernhard
 *
 */
public class BatteryStatus {

    /**
     * Reads the batter status based on the operating system
     * @param OS - This way we know which protocol to use based on the OS
     * @return - the percentage of battery as an integer
     */
	public static int read(String OS){
        try{
            if(OS.equals("Windows")){
                //A batch file to be included in the working directory
                String cmd = "BatteryStatus.bat";

                Process child = Runtime.getRuntime().exec(cmd);



                BufferedReader out = new BufferedReader(new InputStreamReader(child.getInputStream()));

                String s = "";
                //This should be at most one line
                while((s = out.readLine()) != null){
                    //TODO Figure out what actually happens when the batch file returns something that isn't an integer...
                    if(s.contains("error"))
                        return 100;
                    else
                        return Integer.parseInt(s); //format the acpi output to truncate

                }


            } else if(OS.equals("Linux")){

                    String cmd = "acpi -b";
                    ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
                    pb.redirectErrorStream(true);
                    Process child = pb.start();



                    BufferedReader out = new BufferedReader(new InputStreamReader(child.getInputStream()));

                    String s = "";
                    //This should be at most one line
                    while((s = out.readLine()) != null){
                        if(s.contains("power_supply"))
                            return 100;
                        else
                            return Integer.parseInt(s.substring(s.indexOf("%")- 2, s.indexOf("%"))); //format the acpi output to truncate

                    }




             }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return 0;
	}
}