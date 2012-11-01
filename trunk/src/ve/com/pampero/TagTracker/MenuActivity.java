package ve.com.pampero.TagTracker;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

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

        int contentId = Integer.parseInt(i);
        switch (contentId){
            case Contents.CONTENT_ID1:
                ImageButton contentIcon1 = (ImageButton) findViewById(R.id.content_1);
                contentIcon1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //To change body of implemented methods use File | Settings | File Templates.
                        Toast.makeText(MenuActivity.this,
                                "ImageButton is clicked!", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }

    }
}