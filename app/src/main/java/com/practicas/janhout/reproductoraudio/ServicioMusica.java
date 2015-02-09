package com.practicas.janhout.reproductoraudio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class ServicioMusica extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mp;
    public static ArrayList<String> canciones = new ArrayList<>();
    private boolean reproducir;
    private int cancionActual;
    private NotificationCompat.Builder notificacion;
    private NotificationManager gestorNotificacion;

    private enum Estados {
        idle,
        initialized,
        preparing,
        prepared,
        started,
        paused,
        completed,
        stopped,
        end,
        error
    }

    private Estados estado;

    public static final String PLAY = "play";
    public static final String STOP = "stop";
    public static final String ADD = "add";
    public static final String PAUSE = "pause";
    public static final String PREVIOUS = "previous";
    public static final String NEXT = "next";
    public static final String ACTIIVIDAD_PRINCIPAL = "act_principal";
    public static final String CAMBIO = "cambiar";
    public static final String TERMINAR = "terminar";

    public static final int SERVICIO_FOREGROUND = 5;

    /***********************************************************************/
    /*CONSTRUCTOR*/
    /***********************************************************************/

    public ServicioMusica() {
    }

    /***********************************************************************/
    /*METODOS HEREDADOS*/
    /***********************************************************************/

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mp = new MediaPlayer();
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            estado = Estados.idle;
        } else{
            this.stopSelf();
        }

        inicializarNotificacion();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();

            if (action.equals(PLAY)) {
                play();
            } else if (action.equals(STOP)) {
                stopForeground(true);
                stop();
            } else if (action.equals(ADD)) {
                String dato = intent.getStringExtra(getString(R.string.cancion));
                if (dato != null) {
                    add(dato);
                }
            } else if (action.equals(PAUSE)) {
                pause();
            } else if (action.equals(PREVIOUS)) {
                if (canciones.size() > 0)
                    previous();
            } else if (action.equals(NEXT)) {
                if (canciones.size() > 0)
                    next();
            } else if (action.equals(CAMBIO)){
                int numero = intent.getIntExtra(getString(R.string.valor), 0);
                cambiar(numero);
            } else if(action.equals(TERMINAR)){
                terminar();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mp != null) {
            //mp.reset();
            mp.release();
            mp = null;
        }
        super.onDestroy();
    }

    /***********************************************************************/
    /*INTERFAZ PREPARED LISTENER*/
    /***********************************************************************/

    @Override
    public void onPrepared(MediaPlayer mp) {
        estado = Estados.prepared;
        if(reproducir) {
            mp.start();
            rellenarDatos();
            gestorNotificacion.notify(SERVICIO_FOREGROUND, notificacion.build());
            startForeground(SERVICIO_FOREGROUND, notificacion.build());
            estado = Estados.started;
        }
    }

    /***********************************************************************/
    /*INTERFAZ COMPLETED LISTENER*/
    /***********************************************************************/

    @Override
    public void onCompletion(MediaPlayer mp) {
        estado = Estados.completed;
        next();
    }

    /***********************************************************************/
    /*INTERFAZ AUDIO FOCUS*/
    /***********************************************************************/

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                play();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pause();
                break;
        }
    }

    /***********************************************************************/
    /*PRIVADOS DE REPRODUCCION*/
    /***********************************************************************/

    private void add(String cancion) {
        this.canciones.add(cancion);
    }

    private void cambiar(int numero){
        cancionActual = numero;
        cambio();
    }

    private void next(){
        if(cancionActual < canciones.size()-1){
            cancionActual ++;
        } else{
            cancionActual = 0;
        }
        cambio();
    }

    private void pause(){
        if(estado == Estados.started){
            mp.pause();
            estado = Estados.paused;
        }
    }

    private void play() {
        if(canciones != null && canciones.size() > 0) {
            reproducir = true;
            if(estado == Estados.error){
                estado = Estados.idle;
            }
            if(estado == Estados.idle) {
                preparar();
            }
            if(estado == Estados.initialized || estado == Estados.stopped){
                mp.prepareAsync();
                estado = Estados.preparing;
            } else if(estado == Estados.preparing){
            } else if(estado == Estados.prepared || estado == Estados.paused || estado == Estados.completed){
                mp.start();
                estado = Estados.started;
            } else if(estado == Estados.started){
                pause();
            }
        }
    }

    private void previous(){
        if(cancionActual == 0){
            cancionActual = canciones.size()-1;
        }else{
            cancionActual--;
        }
        cambio();
    }

    private void stop() {
        if(estado == Estados.started || estado == Estados.prepared || estado == Estados.paused || estado == Estados.completed){
            mp.seekTo(0);
            mp.stop();
            estado = Estados.stopped;
        }
        reproducir = false;
    }

    private void terminar(){
        if (estado == Estados.error || estado == Estados.stopped){
            this.stopSelf();
        }
    }

    /***********************************************************************/
    /*AUXILIARES*/
    /***********************************************************************/

    private void cambio(){
        mp.reset();
        estado = Estados.idle;
        gestorNotificacion.notify(SERVICIO_FOREGROUND, notificacion.build());
        play();
    }

    private void inicializarNotificacion(){
        gestorNotificacion = (NotificationManager)getSystemService(
                ServicioMusica.NOTIFICATION_SERVICE);

        Intent notificacionIntent = new Intent(this, Principal.class);
        notificacionIntent.setAction(ACTIIVIDAD_PRINCIPAL);
        notificacionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent intentPrincipal = PendingIntent.getActivity(this, 0,
                notificacionIntent, 0);

        Intent previousIntent = new Intent(this, ServicioMusica.class);
        previousIntent.setAction(PREVIOUS);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, ServicioMusica.class);
        playIntent.setAction(PLAY);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent nextIntent = new Intent(this, ServicioMusica.class);
        nextIntent.setAction(NEXT);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        notificacion = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.reproductor)
                .setContentIntent(intentPrincipal)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous,
                        getString(R.string.previous), ppreviousIntent)
                .addAction(android.R.drawable.ic_media_play,
                        getString(R.string.play), pplayIntent)
                .addAction(android.R.drawable.ic_media_next,
                        getString(R.string.next),
                        pnextIntent);
    }

    private void preparar(){
        try {
            mp.setDataSource(canciones.get(cancionActual));
            estado = Estados.initialized;
        } catch (IOException e) {
            estado = Estados.error;
        }
    }

    private void rellenarDatos() {
        try {
            MediaMetadataRetriever metaR;
            metaR = new MediaMetadataRetriever();
            metaR.setDataSource(canciones.get(cancionActual));
            notificacion.setContentText(metaR.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_ARTIST));
            notificacion.setContentTitle(canciones.get(cancionActual).substring(
                    canciones.get(cancionActual).lastIndexOf("/") + 1));
        } catch (Exception e) {
            notificacion.setContentText(getString(R.string.sin_datos));
            notificacion.setContentTitle(getString(R.string.sin_datos));
        }
    }
}