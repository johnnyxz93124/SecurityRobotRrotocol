package com.robotleo.hardware.serial.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Description: 线程池辅助类
 * 
 * @author Seven
 * @Version 1.0.0
 * @Created at 2013-09-22
 * @Modified by xx on xxxx-xx-xx
 */
public class SppThreadHelper {
	private ExecutorService service;
	private static SppThreadHelper manager;
	private static Semaphore semp;
	private SppThreadHelper() {
		service = Executors.newCachedThreadPool();
		// 信号量  每次只能访问一个
		semp= new Semaphore(1);
		// int num = Runtime.getRuntime().availableProcessors();
		// if(num < 2) {
		// service = Executors.newFixedThreadPool(4);
		// } else {
		// service = Executors.newFixedThreadPool(num*2);
		// }
	}
	/**
	 * 获取信号量对象
	 * @return
	 */
	public Semaphore getSemaphore(){
		if(semp==null){
			semp= new Semaphore(1);
		}
		return semp;
	}

	public static synchronized SppThreadHelper getInstance() {
		if (manager == null) {
			manager = new SppThreadHelper();
		}
		return manager;
	}

	public void addTask(Runnable runnable) {
		service.submit(runnable);
	}

}