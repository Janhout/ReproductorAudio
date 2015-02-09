package com.practicas.janhout.reproductoraudio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Principal extends Activity {

    private static final int GET_MUSICA = 1;
    private static final int GRABAR_AUDIO = 2;

    private ListView lista;
    private ArrayAdapter ad;
    private ArrayList<String> canciones;

    /***********************************************************************/
    /*METODOS ON......*/
    /***********************************************************************/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_MUSICA) {
                Uri uri = data.getData();
                String ruta = getPathFromURI(this, uri);
                canciones.add(ruta.substring(ruta.lastIndexOf("/") + 1));
                ad.notifyDataSetChanged();
                Intent intent = new Intent(this, ServicioMusica.class);
                intent.putExtra(getString(R.string.cancion), ruta);
                intent.setAction(ServicioMusica.ADD);
                startService(intent);
            } else if(requestCode == GRABAR_AUDIO) {
                String son = data.getStringExtra(Grabador.FICHERO);
                File origen = new File(son);
                moverFichero(origen);
                origen.delete();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        lista = (ListView)findViewById(R.id.lista_canciones);
        ArrayList<String> a = ServicioMusica.canciones;
        canciones = new ArrayList<>();
        if(a != null ){
            for (int i = 0; i < a.size(); i++) {
                canciones.add(a.get(i).substring(a.get(i).lastIndexOf("/")+1));
            }
        }
        ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, canciones);
        lista.setAdapter(ad);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Principal.this, ServicioMusica.class);
                intent.setAction(ServicioMusica.CAMBIO);
                intent.putExtra(getString(R.string.valor), position);
                startService(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.grabar) {
            grabarAudio();
            pause(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.TERMINAR);
        startService(intent);
    }

    /***********************************************************************/
    /*AUXILIARES*/
    /***********************************************************************/

    private String devolverNombre(){
        SimpleDateFormat formatoFecha = new SimpleDateFormat(getString(R.string.formato_fecha));
        String fecha = formatoFecha.format(new Date());
        return getString(R.string.nombre_fichero) + fecha + getString(R.string.formato);
    }

    public String getPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void grabarAudio() {
        Intent i = new Intent(this, Grabador.class);
        startActivityForResult(i, GRABAR_AUDIO);
    }

    private void moverFichero(File origen){
        String nombre = devolverNombre();
        File fin = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), nombre);
        try {
            FileInputStream inStream = new FileInputStream(origen);
            FileOutputStream outStream = new FileOutputStream(fin);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
        } catch(IOException e){
        }
    }

    /***********************************************************************/
    /*METODOS BOTONES*/
    /***********************************************************************/

    public void add(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(getString(R.string.buscar_audio));
        startActivityForResult(intent, GET_MUSICA);
    }

    public void pause(View v){
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.PAUSE);
        startService(intent);
    }

    public void play(View v) {
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.PLAY);
        startService(intent);
    }

    public void stop(View v) {
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.STOP);
        startService(intent);
    }

    public void next(View v){
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.NEXT);
        startService(intent);
    }

    public void previous(View v){
        Intent intent = new Intent(this, ServicioMusica.class);
        intent.setAction(ServicioMusica.PREVIOUS);
        startService(intent);
    }
}