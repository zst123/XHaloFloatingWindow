package com.zst.xposed.halo.floatingwindow.hooks.ipc;

interface XHFWInterface {
	// Window management
	void bringAppToFront(IBinder token, int taskId);
	int getLastTaskId();
}