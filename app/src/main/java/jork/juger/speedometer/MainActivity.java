package jork.juger.speedometer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView valueTextView = findViewById(R.id.progressTextView);
        final Speedometer speedometer = findViewById(R.id.customPanel);
        final SpeedometerWithRangedValues customRangedSpedometer = findViewById(R.id.customRangedPanel);
        ((SeekBar)findViewById(R.id.progressSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueTextView.setText(String.valueOf(progress));
                speedometer.setValue(progress);
                customRangedSpedometer.setValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
