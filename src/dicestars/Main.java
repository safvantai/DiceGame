/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dicestars;

import com.shephertz.app42.server.AppWarpServer;

/**
 *
 * @author Imtiaz
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
	String appconfigPath = System.getProperty("user.dir")+System.getProperty("file.separator")+"AppConfig.json";
	
        boolean started = AppWarpServer.start(new DiceStarsServerExtension(), appconfigPath);
        
        System.out.println("Server Running...");
//        if(!started){
//            throw new Exception("AppWarpServer did not start. See logs for details.");
//        }
    }
}
