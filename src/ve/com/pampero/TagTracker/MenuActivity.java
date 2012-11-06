package ve.com.pampero.TagTracker;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;

/**
 * Created with IntelliJ IDEA.
 * User: Jose Manuel Aguirre
 * Date: 29/10/12
 * Time: 06:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class MenuActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        String i = (String) getIntent().getExtras().get("id");
        LoggerPrint.INFO("Unlocking Content with ID: " + i);        
        Button buttonEnabled;
        
        
        disableAllButtons();       

        int contentId = Integer.parseInt(i);
        switch (contentId){
        	case Contents.CONTENT_ID1:
            	buttonEnabled = (Button) findViewById(R.id.button1);
            	buttonEnabled.setEnabled(true);
            break;
        	case Contents.CONTENT_ID2:
            	buttonEnabled = (Button) findViewById(R.id.button2);
            	buttonEnabled.setEnabled(true);
            break;
        	case Contents.CONTENT_ID3:
            	buttonEnabled = (Button) findViewById(R.id.button3);
            	buttonEnabled.setEnabled(true);
            break;
        	case Contents.CONTENT_ID4:
            	buttonEnabled = (Button) findViewById(R.id.button4);
            	buttonEnabled.setEnabled(true);
            break;
        	case Contents.CONTENT_ID5:
            	buttonEnabled = (Button) findViewById(R.id.button5);
            	buttonEnabled.setEnabled(true);
            break;
        	case Contents.CONTENT_ID6:
            	buttonEnabled = (Button) findViewById(R.id.button6);
            	buttonEnabled.setEnabled(true);
            break;
        }
        

    }
    
    public void disableAllButtons(){
    	Button button;
    	
    	button = (Button) findViewById(R.id.button1);
        button.setEnabled(false);
        button = (Button) findViewById(R.id.button2);
        button.setEnabled(false);
        button = (Button) findViewById(R.id.button3);
        button.setEnabled(false);
        button = (Button) findViewById(R.id.button4);
        button.setEnabled(false);
        button = (Button) findViewById(R.id.button5);
        button.setEnabled(false);
        button = (Button) findViewById(R.id.button6);
        button.setEnabled(false);
    }
}