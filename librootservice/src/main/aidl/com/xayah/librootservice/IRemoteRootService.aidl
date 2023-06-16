package com.xayah.librootservice;

import com.xayah.librootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    StatFsParcelable readStatFs(String path);
}
