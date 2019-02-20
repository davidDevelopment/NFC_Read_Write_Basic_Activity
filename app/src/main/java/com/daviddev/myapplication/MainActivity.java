package com.daviddev.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MainActivity extends Activity implements View.OnClickListener{

    NfcAdapter nfcAdapter;
    Tag tag;

    Button changeModeButton;
    TextView readText, explanationText;
    EditText writeText;

    boolean readMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Serialize layout objects
        explanationText = findViewById(R.id.explanationText);
        readText = findViewById(R.id.readText);
        writeText = findViewById(R.id.writeText);

        changeModeButton = findViewById(R.id.changeModeButton);
        changeModeButton.setOnClickListener(this);

        //Set default mode to read mode
        changeModeButton.setText("Read mode");
        readMode = true;
        readText.setVisibility(View.VISIBLE);
        writeText.setVisibility(View.INVISIBLE);
        explanationText.setText(R.string.explanationRead);

        //Get the NFC adapter to check if the NFC feature is ok.
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (isNfcEnable(nfcAdapter) != true) {
            Toast.makeText(this, "Please enable NFC feature", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    //Methode to check if NFC feature is enable
    public static boolean isNfcEnable(NfcAdapter nfcAdapter) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled())
            return false;
        else
            return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        //Check if the new intent is an NFC intent
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {

            //Extract tag object from the intent
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            //Check the mode is set on readMode or in writeMode
            if(readMode) {
                //Read the NFC tag
                String payloadString = readNdefMessage(tag);
                //Display it in the readText textBox
                readText.setText(payloadString);
            }
            else
                //Write the NFC tag with the text entered in the writeText textBox
                writeNdefMessage(tag , writeText.getText().toString());
        }
    }

    //Methode to read
    protected String readNdefMessage(Tag tag)
    {

        //Check if the tag object is null
        if (tag == null) {
            Toast.makeText(this, "Tag object cannot be null.", Toast.LENGTH_SHORT).show();
            return "";
        }

        String payloadString = "";

        try {
            /* Methode to extract NdefString from a cached tag:
            * Tag
            *     -> Ndef
            *             -> NdefMessage
            *                            -> NdefRecords
            *                                           -> recordString
            *  V V V The methode is implemented below V V V
            */

            //Extract ndef object from the tag
            Ndef ndef = Ndef.get(tag);

            //Extract ndefmessage from the ndef object
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            //Extract ndef records from the ndef message
            NdefRecord[] records = ndefMessage.getRecords();

            //If records is empty
            if (records.length == 0)
            {
                return "";
            }
            //If records is not empty
            else {
                //Parse records and extract the record string
                for (NdefRecord Record : records) {

                    if (Record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(Record.getType(), NdefRecord.RTD_TEXT)) {
                        //Get payload array from the record
                        byte[] contentpayload = Record.getPayload();

                        String Encoding = ((contentpayload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                        int languageCodeLength = contentpayload[0] & 0063;

                        //Decode payload array and create payloadString.
                        payloadString = new String(contentpayload, languageCodeLength + 1, contentpayload.length - languageCodeLength - 1, Encoding);
                    }
                }
            }

             Toast.makeText(this, "Extraction of the NDEF message.", Toast.LENGTH_LONG).show();
        }
        catch (UnsupportedEncodingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return payloadString;
    }

    private void writeNdefMessage(Tag tag, String writeText) {

        //Check if the tag object is null
        if (tag == null) {
            Toast.makeText(this, "Tag object cannot be null.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            // Translate string text to ndef message
            NdefRecord ndefRecord = NdefRecord.createTextRecord( null, writeText);
            NdefMessage ndefMessage = new NdefMessage(ndefRecord);

            //Acquiring the Ndef object of the tag
            Ndef ndef = Ndef.get(tag);

            //If the tag is not Ndef formatble format it
            if (ndef == null) {
                formatTag(tag, ndefMessage);
            }
            //If the tag is Ndef formatble
            else {
                ndef.connect();
                //Check if the tag is writable
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "\"" + writeText + "\" has been writen.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.i("formatTag: ","error writing ndef message");
        }
    }

    //Methode to format the Tag if it is not in the NDEF format
    private void formatTag(Tag tag, NdefMessage ndefMessage) {

        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "NFC TAG is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

        } catch (Exception e) {
            Log.e("Tag formatting error", e.getMessage());
        }
    }

    public void onClick(View vue) {

        switch (vue.getId()) {

            /**On changeModeButton clic:
             *  if mode == read -> mode = write
             *  if mode == write -> mode = read
             */

            case R.id.changeModeButton:
                if (readMode) {
                    changeModeButton.setText("Write mode");
                    readMode = false;
                    readText.setVisibility(View.INVISIBLE);
                    writeText.setVisibility(View.VISIBLE);
                    explanationText.setText(R.string.explanationWrite);
                }
                else{
                    changeModeButton.setText("Read mode");
                    readMode = true;
                    readText.setVisibility(View.VISIBLE);
                    writeText.setVisibility(View.INVISIBLE);
                    explanationText.setText(R.string.explanationRead);
                }
                break;
        }

    }

    // Disable Catching Nfc Intents
    @Override
    protected void onPause() {
        super.onPause();
        disableCatchingNfcIntents();
    }

    //Enable Catching Nfc intents
    @Override
    protected void onResume() {
        super.onResume();
        // enableCatchingNfcIntents function must be called in onResume methode.
        enableCatchingNfcIntents();
    }

    // MainActivity take the hand on NFC intents
    private void enableCatchingNfcIntents() {

        // FLAG_RECEIVER_REPLACE_PENDING allow the MainActivity to stay in the foreground
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        // Argument one for enableForegroundDIspatch
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // Argument two for enableForegroundDIspatch
        IntentFilter[] intentFilter = new IntentFilter[]{};
        // Allow to handle NFC events in this activity
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    // MainActivity lost the hand on NFC intents
    private void disableCatchingNfcIntents() {
        nfcAdapter.disableForegroundDispatch(this);
    }

}