package com.galarzaa.androidthings.samples;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.galarzaa.androidthings.Rc522;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Rc522 mRc522;
    RfidTask mRfidTask;
    private TextView mTagDetectedView;
    private TextView mTagUidView;
    private TextView mTagResultsView;
    private TextView mTagResultsView1;
    private TextView mTagResultsView2;
    private TextView mTagResultsView3;
    private Button button;

    private SpiDevice spiDevice;
    private Gpio gpioReset;

    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";

    String resultsText = "";
    String resultsText1 = "";
    String resultsText2 = "";
    String resultsText3 = "";
    String te = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTagDetectedView = (TextView) findViewById(R.id.tag_read);
        mTagUidView = (TextView) findViewById(R.id.tag_uid);
        mTagResultsView = (TextView) findViewById(R.id.tag_results);
        mTagResultsView1 = (TextView) findViewById(R.id.tag_results1);
        mTagResultsView2 = (TextView) findViewById(R.id.tag_results2);
        mTagResultsView3 = (TextView) findViewById(R.id.tag_pass);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRfidTask = new RfidTask(mRc522);
                mRfidTask.execute();
                ((Button) v).setText(R.string.reading);
            }
        });
        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            spiDevice = pioService.openSpiDevice(SPI_PORT);
            gpioReset = pioService.openGpio(PIN_RESET);
            mRc522 = new Rc522(spiDevice, gpioReset);
            mRc522.setDebugging(true);
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (spiDevice != null) {
                spiDevice.close();
            }
            if (gpioReset != null) {
                gpioReset.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RfidTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidTask";
        private Rc522 rc522;

        RfidTask(Rc522 rc522) {
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            button.setEnabled(false);
            mTagResultsView.setVisibility(View.GONE);
            mTagResultsView1.setVisibility(View.GONE);
            mTagResultsView2.setVisibility(View.GONE);
            mTagResultsView3.setVisibility(View.GONE);
            mTagDetectedView.setVisibility(View.GONE);
            mTagUidView.setVisibility(View.GONE);
            resultsText = "";
            resultsText1 = "";
            resultsText2 = "";
            resultsText3 = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            rc522.stopCrypto();
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if (!rc522.request()) {
                    continue;
                }
                //Check for collision errors
                if (!rc522.antiCollisionDetect()) {
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                mTagResultsView.setText(R.string.unknown_error);
                mTagResultsView1.setText(R.string.unknown_error);
                mTagResultsView2.setText(R.string.unknown_error);
                mTagResultsView3.setText(R.string.unknown_error);
                return;
            }

            byte addressName = Rc522.getBlockAddress(6, 0);
            byte addressDOB = Rc522.getBlockAddress(6, 1);
            byte addressID = Rc522.getBlockAddress(6,2);
            //byte addressPW = Rc522.getBlockAddress(5, 3);

            byte[] key = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            // Each sector holds 16 bytes

            byte[] newName = {'x', 'F', 'O', 'O', 'D', 'z', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] newDOB = {'1', '4', '/', '7', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] ID = {'1', '5', '5', '2', '1', '7', '0', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
           // byte[] password = {'h', 'i', 'c', 'h','a', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            // In this case, Rc522.AUTH_A or Rc522.AUTH_B can be used
            try {
                //We need to authenticate the card, each sector can have a different password

                boolean resultName = rc522.authenticateCard(Rc522.AUTH_A, addressName, key);
                boolean resultDOB ;
                boolean resultID ;
                boolean resultPass ;

                if (!resultName) {
                    mTagResultsView.setText(R.string.authetication_error);
                    return;
                }


                resultName = rc522.writeBlock(addressName, newName);
                if (!resultName) {
                    mTagResultsView.setText(R.string.write_error);
                    return;
                }
                resultDOB = rc522.writeBlock(addressDOB, newDOB);
                if (!resultDOB) {
                    mTagResultsView1.setText(R.string.write_error);
                    return;
                }
                resultID = rc522.writeBlock(addressID, ID);
                if (!resultID) {
                    mTagResultsView2.setText(R.string.write_error);
                    return;
                }
//                resultPass = rc522.writeBlock(addressPW, password);
//                if (!resultPass) {
//                    mTagResultsView3.setText(R.string.write_error);
//                    return;
//                }

                resultsText += "Sector written successfully";

                byte[] buffName = new byte[16];
                byte[] buffDOB = new byte[16];
                byte[] buffID = new byte[16];
                byte[] buffPass = new byte[16];
                //String a = new String(buffName);
                //Since we're still using the same block, we don't need to authenticate again
                resultName = rc522.readBlock(addressName, buffName);
                if (!resultName) {
                    mTagResultsView.setText(R.string.read_error);
                    return;
                }

                resultDOB = rc522.readBlock(addressDOB, buffDOB);
                if (!resultDOB) {
                    mTagResultsView1.setText(R.string.read_error);
                    return;
                }

                resultID = rc522.readBlock(addressID, buffID);
                if (!resultID) {
                    mTagResultsView2.setText(R.string.read_error);
                    return;
                }
//                resultPass = rc522.readBlock(addressPW, buffPass);
//                if (!resultPass) {
//                    mTagResultsView3.setText(R.string.read_error);
//                    return;
//                }

                //resultsText += "\nSector read successfully: " + Rc522.dataToHexString(buffName);
                resultsText += "\nName: " + new String(buffName);
                rc522.stopCrypto();
                mTagResultsView.setText(resultsText);


                //resultsText2 += "(DOB) Sector written successfully";

                //String a = new String(buffName);
                //Since we're still using the same block, we don't need to authenticate again

                // resultsText += "\nSector read successfully: " + Rc522.dataToHexString(buffName);
                resultsText1 += "DOB: " + new String(buffDOB);
                rc522.stopCrypto();
                mTagResultsView1.setText(resultsText1);

                resultsText2 += "ID: " + new String(buffID);
                rc522.stopCrypto();
                mTagResultsView2.setText(resultsText2);

//                resultsText3 += "\npassword: " + new String(buffPass);
//                rc522.stopCrypto();
//                mTagResultsView3.setText(resultsText3);


            } finally {
                button.setEnabled(true);
                button.setText(R.string.start);
                mTagUidView.setText(getString(R.string.tag_uid, rc522.getUidString()));
                mTagResultsView.setVisibility(View.VISIBLE);
                mTagResultsView1.setVisibility(View.VISIBLE);
                mTagResultsView2.setVisibility(View.VISIBLE);
                mTagResultsView3.setVisibility(View.VISIBLE);
                mTagDetectedView.setVisibility(View.VISIBLE);
                mTagUidView.setVisibility(View.VISIBLE);
            }
        }
    }
}
