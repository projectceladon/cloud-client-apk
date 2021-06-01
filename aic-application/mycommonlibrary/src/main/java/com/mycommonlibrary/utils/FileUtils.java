package com.mycommonlibrary.utils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import java.io.*;

public class FileUtils {

    //检查SDCard存在并且可以读写
    public static boolean isSDCardState() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 判断文件是否已经存在
     *
     * @param fileName 要检查的文件名
     * @return boolean, true表示存在，false表示不存在
     */
    public static boolean isFileExist(String fileName) {
        File file = new File("绝对路径" + fileName);
        return file.exists();
    }

    /**
     * 新建目录
     *
     * @param path 目录的绝对路径
     * @return 创建成功则返回true
     */
    public static boolean createFolder(String path) {
        File file = new File(path);
        return file.mkdir();
    }

    /**
     * 创建文件
     *
     * @param path     文件所在目录的目录名
     * @param fileName 文件名
     * @return 文件新建成功则返回true
     */
    public static boolean createFile(String path, String fileName) {
        File file = new File(path + File.separator + fileName);
        if (file.exists()) {
            return false;
        } else {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean createDir(String path, String dirName) {
        File file = new File(path + File.separator + dirName);
        if (file.exists() || file.isFile())
            return false;
        return file.mkdir();
    }

    /**
     * 删除单个文件
     *
     * @param path     文件所在的绝对路径
     * @param fileName 文件名
     * @return 删除成功则返回true
     */
    public static boolean deleteFile(String path, String fileName) {
        File file = new File(path + File.separator + fileName);
        return file.exists() && file.delete();
    }

    /**
     * 删除一个目录（可以是非空目录）
     *
     * @param dir 目录绝对路径
     */
    public static boolean deleteDirection(File dir) {
        if (dir == null || !dir.exists() || dir.isFile()) {
            return false;
        }
        if (dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                } else if (file.isDirectory()) {
                    deleteDirection(file);//递归
                }
            }
        }
        dir.delete();
        return true;
    }

    /**
     * 读取文件
     *
     * @param sFileName
     * @return
     */
    public static String readFile(String sFileName) {
        if (TextUtils.isEmpty(sFileName)) {
            return null;
        }

        final StringBuffer sDest = new StringBuffer();
        final File f = new File(sFileName);
        if (!f.exists()) {
            return null;
        }
        try {
            FileInputStream is = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            try {
                String data = null;
                while ((data = br.readLine()) != null) {
                    sDest.append(data);
                }
            } catch (IOException ioex) {
                LogEx.e(ioex + "");
                ioex.printStackTrace();
            } finally {
                is.close();
                is = null;
                br.close();
                br = null;
            }
        } catch (Exception ex) {
            LogEx.e(ex + "");
            ex.printStackTrace();
        } catch (OutOfMemoryError ex) {
            ex.printStackTrace();
        }
        return sDest.toString().trim();
    }

    /**
     * 从assets 文件夹中获取文件并读取数据
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String getFromAssets(Context context, String fileName) {
        String result = "";
        try {
            final InputStream in = context.getResources().getAssets()
                    .open(fileName);
            // 获取文件的字节数
            final int lenght = in.available();
            // 创建byte数组
            byte[] buffer = new byte[lenght];
            // 将文件中的数据读到byte数组中
            in.read(buffer);

            result = new String(buffer, "UTF-8");
            in.close();
            buffer = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            LogEx.e(e + "");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 保存文件
     *
     * @param isAppend
     * @return
     */
    public static boolean writeStringToFile(String content, String fileName, boolean isAppend) {
        return writeStringToFile(content, "", fileName, isAppend);
    }

    public static boolean writeStringToFile(String content,
                                            String directoryPath, String fileName, boolean isAppend) {
        if (!TextUtils.isEmpty(content)) {
            if (!TextUtils.isEmpty(directoryPath)) {// 是否需要创建新的目录
                final File threadListFile = new File(directoryPath);
                if (!threadListFile.exists()) {
                    threadListFile.mkdirs();
                }
            }
            boolean bFlag = false;
            final int iLen = content.length();
            final File file = new File(fileName);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                final FileOutputStream fos = new FileOutputStream(file,
                        isAppend);
                byte[] buffer = new byte[iLen];
                try {
                    buffer = content.getBytes();
                    fos.write(buffer);
                    if (isAppend) {
                        fos.write("||".getBytes());
                    }
                    fos.flush();
                    bFlag = true;
                } catch (IOException ioex) {
                    LogEx.e(ioex + "");
                    ioex.printStackTrace();
                } finally {
                    fos.close();
                    buffer = null;
                }
            } catch (Exception ex) {
                LogEx.e(ex + "");
                ex.printStackTrace();
            } catch (OutOfMemoryError o) {
                o.printStackTrace();
            }
            return bFlag;
        }
        return false;
    }

    /**
     * 将字符串写入文件
     *
     * @param text     写入的字符串
     * @param fileStr  文件的绝对路径
     * @param isAppend true从尾部写入，false从头覆盖写入
     */
    public static void writeFile(String text, String fileStr, boolean isAppend) {
        FileOutputStream f = null;
        try {
            File file = new File(fileStr);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            f = new FileOutputStream(fileStr, isAppend);
            f.write(text.getBytes());
            f.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 拷贝文件
     *
     * @param srcPath 绝对路径
     * @param destDir 目标文件所在目录
     * @return boolean true拷贝成功
     */
    public static boolean copyFile(String srcPath, String destDir) {
        boolean flag = false;
        File srcFile = new File(srcPath); // 源文件
        if (!srcFile.exists()) {
            Log.i("FileUtils is copyFile：", "源文件不存在");
            return false;
        }
        // 获取待复制文件的文件名
        String fileName = srcPath.substring(srcPath.lastIndexOf(File.separator));
        String destPath = destDir + fileName;
        if (destPath.equals(srcPath)) {
            Log.i("FileUtils is copyFile：", "源文件路径和目标文件路径重复");
            return false;
        }
        File destFile = new File(destPath); // 目标文件
        if (destFile.exists() && destFile.isFile()) {
            Log.i("FileUtils is copyFile：", "该路径下已经有一个同名文件");
            return false;
        }
        File destFileDir = new File(destDir);
        destFileDir.mkdirs();
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(srcPath);
            fos = new FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int c;
            while ((c = fis.read(buf)) != -1) {
                fos.write(buf, 0, c);
            }
            fis.close();
            fos.close();
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 重命名文件
     *
     * @param oldPath 旧文件的绝对路径
     * @param newPath 新文件的绝对路径
     * @return 文件重命名成功则返回true
     */
    public static boolean renameTo(String oldPath, String newPath) {
        if (oldPath.equals(newPath)) {
            Log.i("FileUtils is renameTo：", "文件重命名失败：新旧文件名绝对路径相同");
            return false;
        }
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);

        return oldFile.renameTo(newFile);
    }

    /**
     * 计算某个文件的大小
     *
     * @param path 文件的绝对路径
     * @return 文件大小
     */
    public static long getFileSize(String path) {
        File file = new File(path);
        return file.length();
    }

    /**
     * 计算某个文件夹的大小
     *
     * @param file 目录所在绝对路径
     * @return 文件夹的大小
     */
    public static double getDirSize(File file) {
        if (file.exists()) {
            //如果是目录则递归计算其内容的总大小
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    double size = 0;
                    for (File f : children)
                        size += getDirSize(f);
                    return size;
                } else {
                    return 0.0;
                }
            } else {//如果是文件则直接返回其大小,以“兆”为单位
                return (double) file.length() / 1024 / 1024;
            }
        } else {
            return 0.0;
        }
    }

    /**
     * 获取某个路径下的文件列表
     *
     * @param path 文件路径
     * @return 文件列表File[] files
     */
    public static File[] getFileList(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                return files;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 计算某个目录包含的文件数量
     *
     * @param path 目录的绝对路径
     * @return 文件数量
     */
    public static int getFileCount(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();
        return files.length;
    }

    /**
     * 获取SDCard 总容量大小(MB)
     *
     * @param path 目录的绝对路径
     * @return 总容量大小
     */
    public long getSDCardTotal(String path) {

        if (null != path && path.equals("")) {

            StatFs statfs = new StatFs(path);
            //获取SDCard的Block总数
            long totalBlocks = statfs.getBlockCount();
            //获取每个block的大小
            long blockSize = statfs.getBlockSize();
            //计算SDCard 总容量大小MB
            return totalBlocks * blockSize / 1024 / 1024;

        } else {
            return 0;
        }
    }

    /**
     * 获取SDCard 可用容量大小(MB)
     *
     * @param path 目录的绝对路径
     * @return 可用容量大小
     */
    public long getSDCardFree(String path) {

        if (null != path && path.equals("")) {

            StatFs statfs = new StatFs(path);
            //获取SDCard的Block可用数
            long availaBlocks = statfs.getAvailableBlocks();
            //获取每个block的大小
            long blockSize = statfs.getBlockSize();
            //计算SDCard 可用容量大小MB
            return availaBlocks * blockSize / 1024 / 1024;

        } else {
            return 0;
        }
    }

    /**
     * 删除文件
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        LogEx.e("deleteFile path " + filePath);

        if (!TextUtils.isEmpty(filePath)) {
            final File file = new File(filePath);
            LogEx.e("deleteFile path exists " + file.exists());
            if (file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    /**
     * 删除文件夹下所有文件
     *
     * @return
     */
    public static void deleteDirectoryAllFile(String directoryPath) {
        final File file = new File(directoryPath);
        deleteDirectoryAllFile(file);
    }

    public static void deleteDirectoryAllFile(File file) {
        if (!file.exists()) {
            return;
        }

        boolean rslt = true;// 保存中间结果
        if (!(rslt = file.delete())) {// 先尝试直接删除
            // 若文件夹非空。枚举、递归删除里面内容
            final File subs[] = file.listFiles();
            if (subs != null) {
                final int size = subs.length - 1;
                for (int i = 0; i <= size; i++) {
                    if (subs[i].isDirectory())
                        deleteDirectoryAllFile(subs[i]);// 递归删除子文件夹内容
                    rslt = subs[i].delete();// 删除子文件夹本身
                }
            }
            // rslt = file.delete();// 删除此文件夹本身
        }

        if (!rslt) {
            LogEx.w("无法删除:" + file.getName());
            return;
        }
    }

    /**
     * 根据后缀名删除文件
     *
     * @param delPath    path of file
     * @param delEndName end name of file
     * @return boolean the result
     */
    public static boolean deleteEndFile(String delPath, String delEndName) {
        // param is null
        if (delPath == null || delEndName == null) {
            return false;
        }
        try {
            // create file
            final File file = new File(delPath);
            if (file != null) {
                if (file.isDirectory()) {
                    // file list
                    String[] fileList = file.list();
                    File delFile = null;

                    // digui
                    final int size = fileList.length;
                    for (int i = 0; i < size; i++) {
                        // create new file
                        delFile = new File(delPath + "/" + fileList[i]);
                        if (delFile != null && delFile.isFile()) {// 删除该文件夹下所有文件以delEndName为后缀的文件（不包含子文件夹里的文件）
                            // if (delFile != null) {//
                            // 删除该文件夹下所有文件以delEndName为后缀的文件（包含子文件夹里的文件）
                            deleteEndFile(delFile.toString(), delEndName);
                        } else {
                            // nothing
                        }
                    }
                } else if (file.isFile()) {

                    // check the end name
                    if (file.toString().contains(".")
                            && file.toString()
                            .substring(
                                    (file.toString().lastIndexOf(".") + 1))
                            .equals(delEndName)) {
                        // file delete
                        file.delete();
                    }
                }
            }
        } catch (Exception ex) {
            LogEx.e(ex.toString());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除文件夹内所有文件
     *
     * @param delpath delpath path of file
     * @return boolean the result
     */
    public static boolean deleteAllFile(String delpath) {
        try {
            // create file
            final File file = new File(delpath);

            if (!file.isDirectory()) {
                file.delete();
            } else if (file.isDirectory()) {

                final String[] filelist = file.list();
                final int size = filelist.length;
                for (int i = 0; i < size; i++) {

                    // create new file
                    final File delfile = new File(delpath + "/" + filelist[i]);
                    if (!delfile.isDirectory()) {
                        delfile.delete();
                    } else if (delfile.isDirectory()) {
                        // digui
                        deleteFile(delpath + "/" + filelist[i]);
                    }
                }
                file.delete();
            }
        } catch (Exception ex) {
            LogEx.e(ex.toString());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     *
     * @param sPath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String sPath) {

        if (TextUtils.isEmpty(sPath)) {
            return false;
        }

        boolean flag;
        // 如果sPath不以文件分隔符结尾，自动添加文件分隔符
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        final File dirFile = new File(sPath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        // 删除文件夹下的所有文件(包括子目录)
        final File[] files = dirFile.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                // 删除子文件
                if (files[i].isFile()) {
                    flag = deleteFile(files[i].getAbsolutePath());
                    if (!flag)
                        break;
                } // 删除子目录
                else {
                    flag = deleteDirectory(files[i].getAbsolutePath());
                    if (!flag)
                        break;
                }
            }
        }
        if (!flag)
            return false;
        // 删除当前目录
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取后缀名
     *
     * @param path 全路径
     * @return
     */
    public static String getFileExtName(String path) {
        String ext = "";
        if ((path != null) && (path.length() > 0)) {
            int dot = path.lastIndexOf('.');
            if ((dot > -1) && (dot < (path.length() - 1))) {
                ext = path.substring(dot + 1);
            }
        }
        return ext;
    }

    /**
     * 获取文件名
     *
     * @param path 全路径
     * @return
     */
    public static String getFileName(String path) {
        if (!TextUtils.isEmpty(path)) {
            return path.substring(path.lastIndexOf(File.separator) + 1);
        }
        return "";
    }

}