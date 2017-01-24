package rawe.gordon.com.record_ffmpeg_android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import rawe.gordon.com.androidrecord.activities.NewRecordVideoActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewRecordVideoActivity.class));
            }
        });
    }
}
