package com.practicas.janhout.reproductoraudio;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class Grabador extends Activity {

    private MediaRecorder grabador;
    private TextView tiempo;
    private Contador c;
    private File temp;

    public static final String FICHERO = "fichero";

    /***********************************************************************/
    /*METODOS ON.....*/
    /***********************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grabador);
        this.setFinishOnTouchOutside(false);
        tiempo = (TextView) findViewById(R.id.tiempo);
        File ruta = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC);
        temp = new File(ruta, "temp.tmp");
        grabar();
    }

    @Override
    public void onBackPressed() {
        this.cancelar(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /***********************************************************************/
    /*BOTONES*/
    /***********************************************************************/

    public void aceptar(View v) {
        terminar();
        Intent i = new Intent();
        i.putExtra(FICHERO, temp.getPath());
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    public void cancelar(View v) {
        terminar();
        if (temp != null) {
            temp.delete();
        }
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /***********************************************************************/
    /*AUXILIARES*/
    /***********************************************************************/

    private void grabar() {
        grabador = new MediaRecorder();
        grabador.setAudioSource(MediaRecorder.AudioSource.MIC);
        grabador.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        grabador.setOutputFile(temp.getPath());
        grabador.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            grabador.prepare();
            grabador.start();
            c = new Contador();
            c.execute();
        } catch (IOException e) {
        }
    }

    private void terminar() {
        if (grabador != null) {
            grabador.stop();
            grabador.release();
            grabador = null;
        }
        c.setGrabar(false);
    }

    /***********************************************************************/
    /*HILO CONTADOR*/
    /***********************************************************************/

    private class Contador extends AsyncTask<Void, Void, Void> {

        private int s;
        private int m;
        private int h;

        private boolean grabar;

        public Contador() {
            s = 0;
            m = 0;
            h = 0;
            grabar = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (grabar) {
                try {
                    Thread.sleep(1000);
                    s = s + 1;
                    publishProgress();
                } catch (InterruptedException e) {
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate();
            if (s == 60) {
                s = 0;
                m = m + 1;
                if (m == 60) {
                    m = 0;
                    h = h + 1;
                }
            }

            String t = ("0" + h).substring(("0" + h).length() - 2, ("0" + h).length()) + ":" +
                    ("0" + m).substring(("0" + m).length() - 2, ("0" + m).length()) + ":" +
                    ("0" + s).substring(("0" + s).length() - 2, ("0" + s).length());
            tiempo.setText(t);
        }

        public void setGrabar(boolean grabar) {
            this.grabar = grabar;
        }
    }
}