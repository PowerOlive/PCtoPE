package com.zhufu.pctope;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.CursorJoiner;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.view.ScrollingView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Result;

import static com.zhufu.pctope.R.id.cards;
import static com.zhufu.pctope.R.id.error_layout;
import static com.zhufu.pctope.R.id.text;
import static com.zhufu.pctope.R.id.unzipping_tip;


public class ConversionActivity extends AppCompatActivity {

    public void MakeErrorDialog(final String errorString){
        //make up a error dialog
        AlertDialog.Builder error_dialog = new AlertDialog.Builder(ConversionActivity.this);
        error_dialog.setTitle(R.string.error);
        error_dialog.setMessage(ConversionActivity.this.getString(R.string.error_dialog)+errorString);
        error_dialog.setIcon(R.drawable.alert_octagram);
        error_dialog.setCancelable(false);
        error_dialog.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        error_dialog.setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ClipboardManager copy = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                copy.setText(errorString);
                finish();
            }
        }).show();
    }


    String path;

    /**
     *
     * @param packRoot
     * @return -1 Unknown
     * @return 0 PE
     * @return 1 PC
     * @return 2 Unknown but can be conversed
     */
    public boolean doVersionDecisions(String packRoot){
        File root = new File(packRoot);
        if(root.exists()){
            File iconPE = new File(root+"/pack_icon.png");
            File iconPC = new File(root+"/pack.png");
            File texturePE = new File(root+"/textures");
            File texturePC = new File(root+"/assets/minecraft/textures");
            Log.d("status","Icon for PE may be at "+iconPE.toString());
            Log.d("status","Icon for PC may be at "+iconPC.toString());
            Log.d("status","Textures for PE may be at "+texturePE.toString());
            Log.d("status","Textures for PC may be at "+texturePC.toString());
            if(iconPE.exists()&&texturePE.exists()){
                Log.d("status","Icon for PE exists.");
                Log.d("status","Textures for PE exist.");
                onPEDecisions(root);
                return true;

            }
            else if(iconPC.exists()&&texturePC.exists()){
                Log.d("status","Icon for PC exists.");
                Log.d("status","Textures for PC exist.");
                onPcDecisions(root);
                return true;
            }
        }
        return false;
    }

    public boolean CopyFileOnSD(String fileFrom, String fileTo){
        File from = new File(fileFrom);
        File to = new File(fileTo);
        if (from.exists()){
            if(to.exists()) {
                to.delete();
                try {
                    to.createNewFile();
                } catch (IOException e) {
                    ErrorsCollector.putError(e.toString(),0);
                    e.printStackTrace();
                    return false;
                }
            }
            try {
                InputStream inputStream = new FileInputStream(fileFrom);
                OutputStream outputStream = new FileOutputStream(fileTo);
                byte[] buffer = new byte[1444];
                int bytesum = 0, byteread = 0;
                while ((byteread = inputStream.read(buffer))!=-1){
                    bytesum += byteread;
                    Log.d("files","[Copying icon] byte = "+bytesum);
                    outputStream.write(buffer, 0, byteread);
                }
                inputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                ErrorsCollector.putError(e.toString(),0);
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                ErrorsCollector.putError(e.toString(),0);
                return false;
            }
            return true;
        }
        else {
            ErrorsCollector.putError("File not found.",0);
            return false;
        }

    }

    //如题，遍历所有子png文件
    static ArrayList<String> filelist = new ArrayList<String>();
    public static ArrayList<String> ListFiles(File path) {
        File files[] = path.listFiles();
        if(files==null) return null;
        for (File f:files){
            if(f.isDirectory()){
                ListFiles(f);
            }
            else{
                String fileindir = "textures",strnow = f.toString();
                int j=0,i;
                for (i=strnow.length()-1;i>=0;i--){
                    if (strnow.charAt(i)=='/') j++;
                    if (j>=2) break;
                }
                fileindir+=strnow.substring(i);
                String lastStr = new String(fileindir.substring(fileindir.lastIndexOf('.')));
                if(lastStr == ".mcmeta"){
                    f.delete();
                    Log.d("files","Deleted .mcmeta file:"+f);
                }
                else{
                    fileindir=fileindir.substring(0,fileindir.indexOf('.'));
                    Log.d("files","NO."+filelist.size()+":"+fileindir+' '+lastStr);
                    filelist.add(fileindir);
                }

            }
        }
        return filelist;
    }

    public void onPcDecisions(File rootPath) {
        File icon = new File(rootPath + "/pack.png");
        File iconPE = new File(rootPath + "/pack_icon.png");
        icon.renameTo(iconPE);//Rename icon to PE
        File texture = new File(rootPath + "/assets/minecraft/textures");
        File texturePE = new File(rootPath + "/textures");
        texture.renameTo(texturePE);//Move textures folder

        //Delete something that we don't need
        new File(rootPath+"/pack.mcmeta").delete();
        DeleteFolder(rootPath+"/assets");

        ArrayList<String> files = ListFiles(rootPath);
        int fileslength = files.size()-1;
        Log.d("files", "Now we have " + fileslength + " files...Writing to textures_list.json...");
        Log.d("files","The first(0) one is "+files.get(0));
        Log.d("files","The final("+fileslength+") one is "+files.get(fileslength));
        File textures_list = new File(rootPath + "/textures/textures_list.json");
        if (fileslength != 0) {
            FileOutputStream out = null;
            try {
                textures_list.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out = new FileOutputStream(textures_list.getPath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                out.write(("[" + System.getProperty("line.separator")).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Write
            for (int i = 0; i <= fileslength; i++){
                try {
                    String fileNow = files.get(i);
                    out.write(("\"" + fileNow + "\"" + "," + System.getProperty("line.separator")).getBytes());
                    Log.d("files","Now i is "+i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                out.write("]".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Write JSONs
        Resources raw = getResources();
        InputStream flipbook = raw.openRawResource(R.raw.flipbook_textures),
                item = raw.openRawResource(R.raw.item_texture),terrain = raw.openRawResource(R.raw.terrain_texture);
        byte[] bflipbook = new byte[3346],bitem = new byte[24356],bterr = new byte[58389];
    }
    public void onPEDecisions(File rootPath){


    }
    public static void unzip(File zipFile, String dest, String passwd) throws ZipException, net.lingala.zip4j.exception.ZipException {
        net.lingala.zip4j.core.ZipFile zFile = new net.lingala.zip4j.core.ZipFile(zipFile); // 首先创建ZipFile指向磁盘上的.zip文件
        zFile.setFileNameCharset("GBK");       // 设置文件名编码，在GBK系统中需要设置
        if (!zFile.isValidZipFile()) {   // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
            throw new ZipException("压缩文件不合法,可能被损坏.");
        }
        File destDir = new File(dest);     // 解压目录
        if (destDir.isDirectory() && !destDir.exists()) {
            destDir.mkdir();
        }
        if (zFile.isEncrypted()) {
            zFile.setPassword(passwd.toCharArray());  // 设置密码
        }
        if(destDir.exists()&&destDir.getTotalSpace() >= zipFile.getTotalSpace()){
            return;
        }
        zFile.extractAll(dest);      // 将文件抽出到解压目录(解压)
    }

    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }
    /**
     * 删除文件夹以及目录下的文件
     * @param   filePath 被删除目录的文件路径
     * @return  目录删除成功返回true，否则返回false
     */
    public boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    /**
     *  根据路径删除指定的目录或文件，无论存在与否
     *@param filePath  要删除的目录或文件
     *@return 删除成功返回 true，否则返回 false。
     */
    public boolean DeleteFolder(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else {
                // 为目录时调用删除目录方法
                return deleteDirectory(filePath);
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Test Area
        //*Code Something for test

        //End of Test

        //Preload
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversion);
        TextInputEditText name = (TextInputEditText) findViewById(R.id.pname);
        TextInputEditText description = (TextInputEditText) findViewById(R.id.pdescription);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final CollapsingToolbarLayout collapsingbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_bar);
        //set default title
        toolbar.setTitle(R.string.project_unnamed);
        //set back button
        setSupportActionBar(toolbar);
        final ActionBar actionbar = getSupportActionBar();
        if(actionbar!=null){
            actionbar.setDisplayHomeAsUpEnabled(true);
        }
        //set project name on changed
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence!=null){
                    collapsingbar.setTitle(charSequence.toString());
                }
                else{
                    String text = getApplicationContext().getResources().getString(R.string.project_unnamed);
                    collapsingbar.setTitle(text);
                }

            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        //set appbar change listener
        //unable
        //set file outof zip
        Intent intent=getIntent();
        final String file = intent.getStringExtra("filePath");

        //Unzip domain
        class UnzippingTask extends AsyncTask<Void, Integer ,Boolean>{
            //==>define
            final LinearLayout unzipping_tip = (LinearLayout) findViewById(R.id.unzipping_tip);
            final LinearLayout cards = (LinearLayout) findViewById(R.id.cards_layout);
            final LinearLayout error_layout = (LinearLayout)findViewById(R.id.error_layout);
            @Override
            protected void onPreExecute(){
                cards.setVisibility(View.GONE);
                unzipping_tip.setVisibility(View.VISIBLE);
                Context context=getApplicationContext();
                path = context.getExternalCacheDir().getPath().toString();
            }

            @Override
            protected Boolean doInBackground(Void... params){
                try {
                    String fileName="";
                    //==>get file name
                    for(int i=file.length()-1;i>=0;i--){
                        if(file.charAt(i)=='/'){
                            Log.d("files","/ is at "+i);
                            for (int j=i+1;j<file.length();j++){
                                fileName+=file.charAt(j);
                            }
                            fileName=fileName.substring(0,fileName.lastIndexOf('.'));
                            break;
                        }
                    }
                    path+="/"+fileName+"/";
                    Log.d("unzip","Unzipping "+file+" to "+path);
                    File fileIn = new File(file);
                    unzip(fileIn,path,"0");
                } catch (Exception e) {
                    ErrorsCollector.putError(e.toString(),0);
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result){
                if(result==true){
                    if(doVersionDecisions(path)){
                        doOnSuccesses();
                    }
                    else {
                        doOnFail();
                    }
                }
                else{
                    //ERRORS
                    //set visibilities
                    cards.setVisibility(View.GONE);
                    unzipping_tip.setVisibility(View.GONE);
                    error_layout.setVisibility(View.VISIBLE);
                    //define
                    FileInputStream in = null;
                    BufferedReader reader = null;
                    final StringBuilder content = new StringBuilder();
                    //Read errors
                    MakeErrorDialog(ErrorsCollector.getError(0));
                }
            }
        }
        new UnzippingTask().execute();
    }


    public void loadIcon(){
        ImageView icon = (ImageView) findViewById(R.id.img_card_icon);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize=2;
        Bitmap bm = BitmapFactory.decodeFile(path.toString()+"/pack_icon.png",options);
        icon.setImageBitmap(bm);
    }


    public void doOnSuccesses(){
        //Set layouts
        //==>define
        final LinearLayout unzipping_tip = (LinearLayout) findViewById(R.id.unzipping_tip);
        final LinearLayout cards = (LinearLayout) findViewById(R.id.cards_layout);
        final LinearLayout error_layout = (LinearLayout)findViewById(R.id.error_layout);
        cards.setVisibility(View.VISIBLE);
        unzipping_tip.setVisibility(View.GONE);
        error_layout.setVisibility(View.GONE);
        LinearLayout cards_layout = (LinearLayout) findViewById(R.id.cards_layout);
        Animation animation = AnimationUtils.loadAnimation(ConversionActivity.this, R.anim.cards_show);
        cards.startAnimation(animation);
        //Load icon
        loadIcon();
        //Set icon editor
        Button edit = (Button) findViewById(R.id.card_icon_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] items = new String[]{ConversionActivity.this.getString(R.string.cover),ConversionActivity.this.getString(R.string.edit_converactdialog)};
                AlertDialog.Builder builder = new AlertDialog.Builder(ConversionActivity.this);
                builder.setTitle(R.string.conversionact_edit_dialog_title);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i){
                            case 0:
                                Intent choose = new Intent(Intent.ACTION_GET_CONTENT);
                                choose.setType("image/*");
                                choose.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(choose, 0);
                        }
                    }
                }).show();
            }
        });
    }

    //onResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == Activity.RESULT_OK)
            if(requestCode == 0){
                Uri uri = data.getData();
                String fileLocation = GetPathFromUri4kitkat.getPath(ConversionActivity.this,uri);
                Log.d("files","Copying image to "+path+"pack_icon.png");
                if(!CopyFileOnSD(fileLocation,path+"pack_icon.png")) MakeErrorDialog(ErrorsCollector.getError(0));
                else{
                    LinearLayout viewC = (LinearLayout)findViewById(R.id.cards);
                    Snackbar.make(viewC,R.string.completed,Snackbar.LENGTH_SHORT).show();
                    loadIcon();
                }
            }
    }


    public void doOnFail(){
        //Delete not pack
        //==>define
        final LinearLayout unzipping_tip = (LinearLayout) findViewById(R.id.unzipping_tip);
        final LinearLayout cards = (LinearLayout) findViewById(R.id.cards_layout);
        final LinearLayout error_layout = (LinearLayout)findViewById(R.id.error_layout);
        cards.setVisibility(View.GONE);
        unzipping_tip.setVisibility(View.GONE);
        error_layout.setVisibility(View.VISIBLE);
        final TextView text = (TextView)findViewById(R.id.error_layout_text);
        text.setText(text.getText()+ConversionActivity.this.getString(R.string.not_pack));
        final File notpack = new File(path);
        Log.d("status","Deleting "+notpack.toString());
        class deleteTask extends AsyncTask<Void , Integer , Boolean>{
            @Override
            protected Boolean doInBackground(Void... voids) {
                Snackbar.make(text,R.string.deleting,Snackbar.LENGTH_LONG).show();
                Boolean r = DeleteFolder(notpack.toString());
                if(r)
                    Snackbar.make(text,R.string.deleted_completed,Snackbar.LENGTH_LONG).show();
                else
                    Snackbar.make(text,R.string.deteted_failed,Snackbar.LENGTH_LONG).show();
                try {
                    Thread.sleep(1400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finish();
                return r;
            }
        }
        new deleteTask().execute();
    }

    //Set BACK Icon
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}