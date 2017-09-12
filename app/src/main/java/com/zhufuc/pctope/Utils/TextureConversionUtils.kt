package com.zhufuc.pctope.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Environment
import android.util.Log

import com.zhufuc.pctope.R

import net.lingala.zip4j.exception.ZipException

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.util.*

/**
 * Created by zhufu on 17-8-28.
 */

class TextureConversionUtils @Throws(FileNotFoundException::class)
constructor(private val FilePath: String, private val context: Context) {

    private fun makeSpace(i: Int): String {
        var spaces = ""
        for (j in 0..i) spaces += " "
        return spaces
    }

    var decisions: PackVersionDecisions? = null
    var VerStr: String? = null

    private fun doVersionDecisions() {
        Log.d("Pack Version", VerStr)
        if (VerStr == TextureCompat.brokenPE || VerStr == TextureCompat.fullPE) {
            onPEDecisions()
        } else if (VerStr == TextureCompat.brokenPC || VerStr == TextureCompat.fullPC) {
            onPcDecisions()
        }
    }

    private fun doPCSpecialResourcesDecisions() {
        //需要特殊处理的贴图包括:
        //滞留药水 无法解决
        //怪物蛋手持贴图 无法解决
        //钟 无法解决
        //指南针 已解决
        //船手持贴图 无法解决
        //北极熊实体 已解决
        //豹猫 已解决
        //僵尸贴图错误 已解决
        //僵尸猪人实体 已解决
        //剥皮者贴图错误 已解决
        //卫道士实体
        //唤魔者实体
        //威克斯
        //箱子手持贴图及大箱子实体 无法解决<>已解决
        //活塞 无法解决
        //方块破坏崩裂贴图 已解决

        val FROM = arrayOf(File(path + "/textures/entity/chest/normal_double.png"), File(path + "/textures/items/dragon_breath.png"), File(path + "/textures/entity/bear/polarbear.png"), File(path + "/textures/entity/cat/black.png"), File(path + "/textures/entity/zombie_pigman.png"))
        val TO = arrayOf(File(path + "/textures/entity/chest/double_normal.png"), File(path + "/textures/items/dragons_breath.png"), File(path + "/textures/entity/polarbear.png"), File(path + "/textures/entity/cat/blackcat.png"), File(path + "/textures/entity/pig/pigzombie.png"))
        for (i in TO.indices) {
            FROM[i].renameTo(TO[i])
        }


        //For compasses
        if (File(path + "/textures/items/compass_00.png").exists()) {

            var imageHeight = 0

            val compasses = ArrayList<Bitmap>()
            for (i in 0..31) {
                val format = DecimalFormat("00")
                val image = File(path + "/textures/items/compass_" + format.format(i) + ".png")
                if (image.exists()) {
                    val now = BitmapFactory.decodeFile(image.path)
                    compasses.add(now)
                    imageHeight += now.height
                    image.delete()
                }
            }

            val bitmap = Bitmap.createBitmap(compasses[0].width, imageHeight, compasses[0].config)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(compasses[0], Matrix(), null)

            for (i in 1..compasses.size - 1) {
                canvas.drawBitmap(compasses[i], 0f, (compasses[i - 1].height * i).toFloat(), null)
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            File(path + "/textures/items/compass_atlas.png").writeBytes(byteArrayOutputStream.toByteArray())
        }

        //For zombies
        val images = arrayOf(File(path + "/textures/entity/zombie/zombie.png"), File(path + "/textures/entity/zombie/husk.png"), File(path + "/textures/entity/pig/pigzombie.png"))
        for (image in images) {
            if (image.exists()) {
                val zombieImage = BitmapFactory.decodeFile(image.path)

                if (zombieImage.height == zombieImage.width) {
                    val bitmap = Bitmap.createBitmap(zombieImage, 0, 0, zombieImage.width, zombieImage.height / 2)

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    image.writeBytes(byteArrayOutputStream.toByteArray())

                }

            }
        }

        //For destroy stages
        for (i in 0..9) {
            val stage = File("$path/textures/blocks/destroy_stage_$i.png")
            if (stage.exists())
                stage.renameTo(File(path + "/textures/environment/" + stage.name))
        }
    }

    private fun onPcDecisions() {
        val icon = File(path + "/pack.png")
        val iconPE = File(path + "/pack_icon.png")
        icon.renameTo(iconPE)//Rename icon to PE
        val texture = File(path + "/assets/minecraft/textures")
        val texturePE = File(path + "/textures")
        texture.renameTo(texturePE)//Move textures folder

        //Delete something that we don't need
        File(path + "/pack.mcmeta").delete()
        DeleteFolder.Delete(path + "/assets")

        doPCSpecialResourcesDecisions()

        val files = ListFiles(File(path))
        val fileslength = files!!.size - 1
        Log.d("files", "Now we have $fileslength files...Writing to textures_list.json...")
        Log.d("files", "The first(0) one is " + files[0])
        Log.d("files", "The final(" + fileslength + ") one is " + files[fileslength])
        val textures_list = File(path + "/textures/textures_list.json")
        if (fileslength != 0) {
            var out: FileOutputStream? = null
            try {
                textures_list.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                out = FileOutputStream(textures_list.path)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            try {
                out!!.write(("[" + System.getProperty("line.separator")).toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //Write
            for (i in 0..fileslength) {
                try {
                    val fileNow = files[i]
                    out!!.write(("\"" + fileNow + "\"" + "," + System.getProperty("line.separator")).toByteArray())
                    Log.d("files", "Now i is " + i)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            try {
                out!!.write("]".toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                out!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

    }


    private fun onPEDecisions() {
        val JSONs = File(path + "/textures/").listFiles()
        for (f in JSONs) {
            val n = f.path
            if (n.substring(n.lastIndexOf('.'), n.length) == ".json")
                f.delete()
        }
    }

    private fun doJsonFixing(terrainTxt: String, SearchFrom: Int): String {
        val terrainText = ArrayList<String>()
        var j = 0

        for (i in 0..terrainTxt.length - 1) {
            if (terrainTxt[i] == '\n') {
                val line = terrainTxt.substring(j, i + 1)
                terrainText.add(line)
                j = i + 1
            }
        }

        for (i in SearchFrom..terrainText.size - 1) {
            val str = terrainText[i]
            if (str.contentEquals("texture/")){
                var first: Int = -1
                var last: Int = -1

                for (j in i..0)
                    if (terrainText[i].contentEquals("{"))
                        first = j

                for (j in i until terrainText.size)
                    if (terrainText[j].contentEquals("}"))
                        last = j

                for (d in first..last)
                    terrainText.removeAt(d)
            }
        }
        val string = StringBuilder()
        string.append(terrainText)

        return string.toString()
    }

    @Throws(FileNotFoundException::class)
    private fun doJSONWriting() {
        //==>define
        //for basic information
        val raw = context.resources
        val data = arrayOf(raw.openRawResource(R.raw.items_client), raw.openRawResource(R.raw.blocks), raw.openRawResource(R.raw.flipbook_textures), raw.openRawResource(R.raw.item_texture), raw.openRawResource(R.raw.terrain_texture))
        val pathes = arrayOf(File(path + "/items_client.json"), File(path + "/blocks.json"))

        for (i in pathes.indices){
            pathes[i].writeBytes(data[i].readBytes(DEFAULT_BUFFER_SIZE))
        }

        //for terrain block textures
        val textBefore = "{" + System.getProperty("line.separator") + makeSpace(4) + "\"resource_pack_name\":" + "\"" + packname + "\"" + System.getProperty("line.separator")
        val terrainOut = FileOutputStream(path + "/textures/terrain_texture.json")
        try {
            terrainOut.write(textBefore.toByteArray())
            terrainOut.write(doJsonFixing(data[4].reader(Charset.defaultCharset()).readText(), 77).toByteArray())
        } catch (e: IOException) {
            mOnCrashListener!!.onCrash(e.toString())
            e.printStackTrace()
        }

        //for item texture file

        val itemOut = FileOutputStream(path + "/textures/item_texture.json")
        var isCreated: Boolean? = true
        val temp = arrayOf(path + "/textures/item_texture.json", path + "/textures/flipbook_textures.json", path + "/manifest.json")
        for (i in temp.indices) {
            val t = File(temp[i])
            if (!t.exists())
                try {
                    if (!t.createNewFile())
                        isCreated = false
                } catch (e: IOException) {
                    mOnCrashListener!!.onCrash(e.toString())
                    e.printStackTrace()
                }

        }
        if (isCreated!!) {
            try {
                itemOut.write(textBefore.toByteArray())
                itemOut.write(doJsonFixing(data[3].reader(Charset.defaultCharset()).readText(), 5).toByteArray())
                itemOut.close()
            } catch (e: IOException) {
                mOnCrashListener!!.onCrash(e.toString())
                e.printStackTrace()
            }

            //for flip book texture
            val flipOut = FileOutputStream(path + "/textures/flipbook_textures.json")
            try {
                flipOut.write(doJsonFixing(data[2].reader(Charset.defaultCharset()).readText(), 2).toByteArray())
                flipOut.close()
            } catch (e: IOException) {
                mOnCrashListener!!.onCrash(e.toString())
                e.printStackTrace()
            }

            //for manifest file

            val intro: String
            val out = JSONObject()
            val versionArray = JSONArray()

            try {
                versionArray.put(0)
                versionArray.put(0)
                versionArray.put(1)

                out.put("format_version", 1)
                val header = JSONObject()
                header.put("description", packdescription)
                header.put("name", packname)
                header.put("uuid", UUID.randomUUID().toString())
                header.put("version", versionArray)
                out.put("header", header)

                val modules = JSONArray()
                val modulesObjs = JSONObject()
                modulesObjs.put("description", packdescription)
                modulesObjs.put("type", "resources")
                modulesObjs.put("uuid", UUID.randomUUID().toString())
                modulesObjs.put("version", versionArray)
                modules.put(modulesObjs)
                out.put("modules", modules)

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            intro = JsonFormatTool().formatJson(out.toString())

            File(path + "/manifest.json").writeText(intro, Charset.defaultCharset())

        } else
            mOnCrashListener!!.onCrash("Could not create JSON files.")

    }

    var compressFinalSize = 0
    private fun doMainInCompressing(n: File) {
        if (n.isFile) {
            //Show progress
            Log.d("compression", "Compressing " + n)
            mConversionChangeListener!!.inDoingImageCompressions(n.path)


            val str = n.path
            if (str.substring(str.lastIndexOf("."), str.length) != ".png")
                return

            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            val image = BitmapFactory.decodeFile(n.path, options)
            //get compressed bitmap
            var compressHeight = compressFinalSize
            var compressWidth = compressFinalSize
            if (image.width - image.height < -5) {
                compressHeight = compressHeight * (image.width / compressFinalSize)
            } else if (image.height - image.width > 5) {
                compressWidth = compressWidth * (image.height / compressFinalSize)
            }
            val compressed = CompressImage.getBitmap(image, compressHeight, compressWidth)

            val baos = ByteArrayOutputStream()
            compressed!!.compress(Bitmap.CompressFormat.PNG, 100, baos)//png

            n.delete()
            n.writeBytes(baos.toByteArray())

        }
    }

    private fun doImageCompressions() {
        if (compressFinalSize == 0)
            return
        Log.i("Pack Conversion", "Doing image compressions...")
        //get images
        val items = File(path + "/textures/items").listFiles()
        val blocks = File(path + "/textures/blocks").listFiles()
        if (items != null) {
            for (n in items)
                doMainInCompressing(n)
        }
        if (blocks != null) {
            for (n in blocks)
                doMainInCompressing(n)
        }
    }

    private fun searchAndOverwrite(n: File) {
        val bitmap = BitmapFactory.decodeFile(n.path)
        for (x in 1..bitmap.width - 1 - 1)
            for (y in 1..bitmap.height - 1 - 1) {

            }
        try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)

            n.writeBytes(baos.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun doImageBlackIntoTransparent() {
        val items = File(path + "/textures/items").listFiles()
        val blocks = File(path + "/textures/blocks").listFiles()
        val entity = File(path + "/textures/entity").listFiles()
        if (items != null)
            for (n in items)
                searchAndOverwrite(n)
        if (blocks != null)
            for (n in blocks)
                searchAndOverwrite(n)
        if (entity != null)
            for (n in entity)
                searchAndOverwrite(n)

    }

    var path: String
    private var packname: String? = null
    private var packdescription: String? = null
    var skipUnzip: Boolean = false

    init {
        this.path = context.externalCacheDir!!.path

        if (!skipUnzip) {
            if (!File(FilePath).exists()) {
                throw FileNotFoundException(context.getString(R.string.crash_converstion_input_file_not_found))
            }
            val fileName = FilePath.substring(FilePath.lastIndexOf('/') + 1, FilePath.lastIndexOf('.'))
            //==>get file name
            path += "/" + fileName
            Log.d("unzip", "We will unzip $FilePath to $path")
        } else
            path = FilePath
        Log.i("status", "Path:" + path)
    }

    /*
        Some Listeners...
     */
    private var mOnUncompressListener: OnUncompressListener? = null

    interface OnUncompressListener {
        fun onPreUncompress()
        fun inUncompressing()
        fun onPostUncompress(result: Boolean, version: String?)
    }

    private var mOnCrashListener: OnCrashListener? = null

    interface OnCrashListener {
        fun onCrash(errorContent: String)
    }

    fun setOnUncompressListener(listener: OnUncompressListener) {
        this.mOnUncompressListener = listener
    }

    fun setOnCrashListener(listener: OnCrashListener) {
        this.mOnCrashListener = listener
    }

    private var mConversionChangeListener: ConversionChangeListener? = null

    interface ConversionChangeListener {
        fun inDoingVersionDecisions()
        fun inDoingImageCompressions(whatsBeingCompressing: String)
        fun inDoingImageColorTurning()
        fun inDoingJSONWriting()
        fun onDone()
    }

    fun setConversionChangeListener(listener: ConversionChangeListener) {
        this.mConversionChangeListener = listener
    }

    fun UncompressPack() {
        class UnzippingTask : AsyncTask<Void, Int, Boolean>() {

            override fun onPreExecute() {
                mOnUncompressListener!!.onPreUncompress()
            }

            override fun doInBackground(vararg params: Void): Boolean? {
                if (!skipUnzip) {
                    mOnUncompressListener!!.inUncompressing()
                    try {
                        Log.d("unzip", "Unzipping to " + path)
                        unzip(File(FilePath), path, "0")
                    } catch (e: Exception) {
                        return false
                    }

                }
                return isPathUseful//Find the true root path
            }

            private val isPathUseful: Boolean?
                get() {
                    val pathDecisions = File(path)
                    if (pathDecisions.exists() && pathDecisions.isDirectory) {
                        val FileListInPath = pathDecisions.listFiles()
                        val Dirs = ArrayList<File>()
                        var FilesFound = 0
                        var DirsFound = 0
                        for (test in FileListInPath) {
                            if (test.isFile)
                                FilesFound++
                            else if (test.isDirectory) {
                                Dirs.add(test)
                                DirsFound++
                            }
                        }
                        if (FilesFound >= 1 && DirsFound >= 1) {
                            return true
                        } else {
                            var isFoundNext: Boolean? = false
                            for (i in Dirs.indices) {
                                path = Dirs[i].path
                                if (isPathUseful!!) {
                                    isFoundNext = true
                                    return true
                                }
                            }
                            if (!(isFoundNext)!!) {
                                return false
                            }
                        }
                    }
                    return false
                }


            override fun onPostExecute(result: Boolean?) {
                if (result!!) {
                    decisions = PackVersionDecisions(File(path))
                    VerStr = decisions!!.packVersion
                    if (VerStr!![0] != 'E') {
                        iconPath = if (VerStr == TextureCompat.fullPC) path + "/pack.png" else if (VerStr == TextureCompat.fullPE) path + "/pack_icon.png" else null
                        mOnUncompressListener!!.onPostUncompress(true, VerStr)
                    } else {
                        mOnUncompressListener!!.onPostUncompress(false, null)
                    }
                } else
                    mOnUncompressListener!!.onPostUncompress(false, null)
            }
        }

        UnzippingTask().execute()
    }

    fun doConverting(packname: String, packdescription: String) {
        this.packname = packname
        this.packdescription = packdescription

        Log.i("Pack Conversion", "Doing version decisions...")
        mConversionChangeListener!!.inDoingVersionDecisions()
        doVersionDecisions()

        doImageCompressions()

        //mConversionChangeListener.inDoingImageColorTurning();
        //doImageBlackIntoTransparent();
        try {
            Log.i("Pack Conversion", "Doing json Writing")
            mConversionChangeListener!!.inDoingJSONWriting()
            doJSONWriting()
        } catch (e: FileNotFoundException) {
            mOnCrashListener?.onCrash(e.toString())
            e.printStackTrace()
        }

        //For if icon doesn't exist
        val iconTest = File(path + "/pack_icon.png")
        if (!iconTest.exists()) {
            Log.i("Pack Conversion", "Writing icon for non-icon-pack...")
            val buffer = ByteArray(1444)
            var i: Int
            val inputStream = context.resources.openRawResource(R.raw.bug_pack_icon)

            File(path + "/pack_icon.png").writeBytes(inputStream.readBytes(DEFAULT_BUFFER_SIZE))
        }

        //Move to dest
        Log.i("Pack Conversion", "Moving to dest...")
        val dest = File(Environment.getExternalStorageDirectory().toString() + "/games/com.mojang/resource_packs/" + packname)
        if (dest.isDirectory && dest.exists()) dest.mkdirs()
        File(path).renameTo(dest)

        mConversionChangeListener!!.onDone()
        //Done
    }

    private var iconPath: String? = null
    var icon: Bitmap?
        get() = if (iconPath == null || !File(iconPath!!).exists()) null else BitmapFactory.decodeFile(iconPath)
        set(icon) {
            if (iconPath != null) {
                try {
                    val baos = ByteArrayOutputStream()
                    File(iconPath).writeBytes(baos.toByteArray())
                } catch (e: IOException) {
                    e.printStackTrace()
                    mOnCrashListener!!.onCrash(e.toString())
                }

            }
        }

    companion object {


        //如题，遍历所有子png文件
        private val filelist = ArrayList<String>()

        private fun ListFiles(path: File): ArrayList<String>? {
            val files = path.listFiles() ?: return null
            for (f in files) {
                if (f.isDirectory) {
                    ListFiles(f)
                } else {
                    var fileindir = "textures"
                    val strnow = f.toString()
                    var j = 0
                    var i: Int
                    i = strnow.length - 1
                    while (i >= 0) {
                        if (strnow[i] == '/') j++
                        if (j >= 2) break
                        i--
                    }
                    fileindir += strnow.substring(i)
                    val lastStr = fileindir.substring(fileindir.lastIndexOf('.'))
                    if (lastStr == ".png") {
                        fileindir = fileindir.substring(0, fileindir.indexOf('.'))
                        Log.d("files", "NO." + filelist.size + ":" + fileindir + ' ' + lastStr)
                        filelist.add(fileindir)
                    }
                }
            }
            return filelist
        }

        @Throws(ZipException::class, net.lingala.zip4j.exception.ZipException::class)
        private fun unzip(zipFile: File, dest: String, passwd: String) {
            val zFile = net.lingala.zip4j.core.ZipFile(zipFile) // 首先创建ZipFile指向磁盘上的.zip文件
            if (!zFile.isValidZipFile) {   // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
                throw ZipException("压缩文件不合法,可能被损坏.")
            }
            val destDir = File(dest)// 解压目录
            if (destDir.exists()) {
                DeleteFolder.Delete(dest)
            }
            destDir.mkdirs()
            if (zFile.isEncrypted) {
                zFile.setPassword(passwd.toCharArray())  // 设置密码
            }
            zFile.extractAll(dest)      // 将文件抽出到解压目录(解压)
        }
    }

}