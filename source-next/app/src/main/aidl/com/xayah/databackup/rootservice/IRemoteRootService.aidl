package com.xayah.databackup.rootservice;

import com.xayah.databackup.parcelables.BytesParcelable;
import com.xayah.databackup.parcelables.StatFsParcelable;
import com.xayah.databackup.parcelables.FilePathParcelable;
import com.xayah.databackup.rootservice.ICallback;

interface IRemoteRootService {
    void testConnection();
    ParcelFileDescriptor getInstalledAppInfos();
    ParcelFileDescriptor getInstalledAppStorages();
    List<UserInfo> getUsers();
    List<BytesParcelable> getPrivilegedConfiguredNetworks();
    int[] addNetworks(in List<BytesParcelable> configs);
    StatFsParcelable readStatFs(String path);
    List<FilePathParcelable> listFilePaths(String path, boolean listFiles, boolean listDirs);
    ParcelFileDescriptor readText(String path);
    void writeText(String path, in ParcelFileDescriptor pfd);
    long calculateTreeSize(String path);
    int callTarCli(String stdOut, String stdErr, in String[] argv);
    List<String> getPackageSourceDir(String packageName, int userId);
    String compress(int level, String inputPath, String outputPath, ICallback callback);
    boolean mkdirs(String path);
    boolean exists(String path);
    boolean deleteRecursively(String path);
    boolean copyRecursively(String source, String target, boolean overwrite);

    // Rustic
    void initRusticRepository(String repositoryPath, String password);
    boolean rusticRepositoryExists(String repositoryPath);
    void validateRusticRepository(String repositoryPath, String password);
    String createRusticSnapshot(String repositoryPath, String password, in Map<String, String> sourcePaths, in List<String> tags, ICallback callback);
    ParcelFileDescriptor readRusticSnapshotTextFiles(String repositoryPath, String password, String snapshotId, in List<String> paths);
    ParcelFileDescriptor deleteRusticSnapshot(String repositoryPath, String password, String snapshotId);
    ParcelFileDescriptor listRusticSnapshots(String repositoryPath, String password);
    void restoreRusticSnapshot(String repositoryPath, String password, String snapshotId, String destinationPath);
    void checkRusticRepository(String repositoryPath, String password);
    void restoreRusticAppApk(String repositoryPath, String password, String snapshotId, String packageName, int userId, in List<String> apkPaths);
    void restoreRusticAppInternalData(String repositoryPath, String password, String snapshotId, String packageName, int userId, int sourceUserId, in List<String> internalDataPaths);
    void restoreRusticAppExternalData(String repositoryPath, String password, String snapshotId, String packageName, int userId, int sourceUserId, in List<String> externalDataPaths);
    List<String> restoreRusticNetworks(String repositoryPath, String password, String snapshotId, in List<String> networkIds);
}
