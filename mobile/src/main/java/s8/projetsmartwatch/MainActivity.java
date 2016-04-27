package s8.projetsmartwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Timer;

public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private static String WEAR_MESSAGE_PATH = "/message";
    private final static String START_ACTIVITY = "/start/activity";

    private GoogleApiClient mApiClient;
    private GoogleApiClient mApiC;

    boolean p1Reponse = false;
    boolean p2Reponse = false;

    TextView textViewP1Reponse;

    Button bYes ;
    Button bNo ;
    String messageRecu = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scenario1);
        bYes = (Button)findViewById(R.id.buttonYesScenario1P2);
        bNo = (Button) findViewById(R.id.buttonNoScenario1P2);

        mApiC = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mApiC.connect();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())) {
            mApiClient.connect();
        }
        textViewP1Reponse = (TextView) findViewById(R.id.textView);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        sendMessage(START_ACTIVITY, "");
        Wearable.MessageApi.addListener(mApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void sendMessage(final String path, final String message){
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiC).await();
                for(Node node : nodes.getNodes()){
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiC, node.getId(), path, message.getBytes()).await();
                }
            }
        }).start();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        messageRecu = new String(messageEvent.getData());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s = " " ;
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
                    System.out.println("mobile message received : " + messageEvent.getPath().toString());
                    s = new String(messageEvent.getData());
                    System.out.println(s);
                    if (s.equalsIgnoreCase("alarmeVisuelle")) {
                        alarmeVisuelle();
                    }
                    if (s.equalsIgnoreCase("alarmeVibrante")) {
                        alarmeVibrante();
                    }
                    if (s.equalsIgnoreCase("alarmeTotale")) {
                        alarmeTotale();
                    }
                    if(s.equalsIgnoreCase("notOk") ){
                        textViewP1Reponse.setText(messageRecu);
                    }
                    //reception d'un message P1 lors d'un probleme de santé de P2
                    if(s.equalsIgnoreCase("P2IsnotOk") ){
                        System.out.println("Alarme P2 is not OK");
                        textViewP1Reponse.setText("P2isNotOk");
                        messageRecu= "P2IsnotOk";
                        p1Reponse =true;
                    }
                    if(s.equalsIgnoreCase("uSeemOk") ){

                        messageRecu = "uSeemOk";
                        textViewP1Reponse.setText("P2 Seem Ok");
                       p1Reponse = true;
                    }
                    textViewP1Reponse.setText(messageRecu);
                }

            }
        });
    }

    public void alarme(View view){
        System.out.println("Envoie d'alarme au tel ");
        sendMessage(WEAR_MESSAGE_PATH, "alarme");
    }


    public void changeLayout(View view) {
        setContentView(R.layout.scenario1);
    }

    public void alarmeVibrante(){
        System.out.println("je vibre");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(1000);
    }

    public void alarmeVisuelle(){
        setContentView(R.layout.alarme);
    }

    public void alarmeTotale(){
        alarmeVibrante();
        alarmeVisuelle();
        sonOn();
        int i=0;
        while(i<1000000)
            i++;
       // sonOff();
    }

    public void sonOn(){
        sendMessage(WEAR_MESSAGE_PATH,"allumer");
    }

    public void sonOff(){
        System.out.println("son off");
        sendMessage(WEAR_MESSAGE_PATH, "eteindre");
    }

    public void switchScenario2(View v){
        Intent intent = new Intent(MainActivity.this, Scenario2Activity.class);
        startActivity(intent);
        finish();
    }


    //envois de l'affichage de are u ok au P1
    public void scenario1HealthP1(View v){
        //TODO pour le pilote 1
         sendMessage(WEAR_MESSAGE_PATH, "areUOK");
    }

    //affichage des boutons oui et non
    public void scenario1HealthP2(View v){
        sendMessage(WEAR_MESSAGE_PATH,"P2ProblemeHealth");
        showButton();
    }

    public void buttonYesScenario1P2(View v){
        hideButton();
        sendMessage(WEAR_MESSAGE_PATH,"P2IsNotOk");

    }


    public void buttonNoScenario1P2(View v){
        hideButton();
        Timer timer ;

        sendMessage(WEAR_MESSAGE_PATH,"P2IsOk");
        hideButton();
        if(!p1Reponse){
            new CountDownTimer(30000, 1000) {

                public void onTick(long millisUntilFinished) {
                    textViewP1Reponse.setText("waiting for P1... : " + (30 - millisUntilFinished/1000) );
                    //P1 repond que  P2 va bien
                    if(messageRecu.equals("uSeemOk")){
                        hideButton();
                        cancel();
                        messageRecu = "";
                        p1Reponse = true;
                        sendMessage(WEAR_MESSAGE_PATH,"allIsfine");
                        //P1 repond que P2 ne va pas bien
                    }else if(messageRecu.equals("notOk") ) {
                        cancel();
                        hideButton();
                        messageRecu = "";
                        System.out.println("ALARME TOTALE SUR MONTRE P2");
                        p1Reponse = true;
                        //TODO a voir?
                        //sendMessage(WEAR_MESSAGE_PATH,"alarmeTotale");
                    }
                }

                public void onFinish() {
                    textViewP1Reponse.setText("Fin du temps " );
                    hideButton();
                    System.out.println("ALARME TOTALE SUR MONTRE P2");
                    sendMessage(WEAR_MESSAGE_PATH,"P2NoReponse");
                    messageRecu = "";
                }
            }.start();
        }

        textViewP1Reponse.setText(" en attente d'un message ... ");

    }


    public void hideButton(){
        bYes.setVisibility(View.INVISIBLE);
        bNo.setVisibility(View.INVISIBLE);

    }
    public void showButton(){
        bYes.setVisibility(View.VISIBLE);
        bNo.setVisibility(View.VISIBLE);
    }


    public void scenario1SendAlert(View v){
        sendMessage(WEAR_MESSAGE_PATH,"alarmeTotale");
    }
}
