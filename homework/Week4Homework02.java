package homework;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 作业： 思考有多少种方式，在 main 函数启动一个新线程，运行一个方法，拿到这 个方法的返回值后，退出主线程?
 * 
 * @author liuby
 *
 */
public class Week4Homework02 {

	public static void main(String[] args) throws InterruptedException, ExecutionException, BrokenBarrierException {
		long start = System.currentTimeMillis();
		
		useFuture();
//		useFutureTask();
//		useCompletableFuture();
//		useJoin();
//		useLock();
//		useLockSupport();
//		useObjectWait();
//		useGlobalVar();
//		useCountDownLatch();
//		useCyclicBarrier();
		
		// 没想出来，但感觉应该可以的。
//		useSemaphore();
//		useSynchronized();
		
		System.out.println("异步计算结果为：" + result);
		System.out.println("使用时间：" + (System.currentTimeMillis() - start) + "ms");
	}

	static Integer result = 0;
	
	static boolean finish = false;
	
	static Object lock = new Object();
	
	/**
	 * 使用Callable返回Future，获得新线程的返回值
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void useFuture() throws InterruptedException, ExecutionException {
		Future<Integer> future = Executors.newCachedThreadPool().submit(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				System.out.println("返回运算结果：1");
				return 1;
			}
		});
		result = future.get();
	}
	
	/**
	 * 将Callable包装秤FutureTask，异步完成任务，返回返回值。
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void useFutureTask() throws InterruptedException, ExecutionException {
		FutureTask<Integer> task = new FutureTask<>(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				return 1;
			}
		});
		//使用Thread
//		new Thread(task).start();;
//		result = task.get();
		
		//使用线程池
		Executors.newCachedThreadPool().submit(task);
		result = task.get();
	}
	
	/**
	 * 使用Java8新特性CompletableFuture异步获得计算结果，并返回计算结果。
	 */
	public static void useCompletableFuture() {
//		CompletableFuture.supplyAsync(()->{return 1;}).thenApply(v -> result = v);
		CompletableFuture.supplyAsync(()->{ return 1;}).thenAccept(v -> result = v);
	}
	
	/**
	 * 通过使用Object的wait，使主线程阻塞，当新线程完成任务后将主线程唤醒，返回计算结果。
	 * @throws InterruptedException
	 */
	public static void useObjectWait() throws InterruptedException {
		new Thread(()->{
			synchronized(lock) {
				result = 1;
				lock.notifyAll();
			}
		}) .start();
		synchronized(lock) {
			lock.wait();
		}
	}
	
	/**
	 * 没想出来，使用Synchronized只能使两个线程先后进入同一代码块，却无法控制前后顺序，main先进入则无法获得新线程计算结果。
	 */
	public static void useSynchronized() {
		new Thread(() -> {
			addOrRead(Operation.ADD, 1);
		}).start();
		result = addOrRead(Operation.READ);
	}
	
	public synchronized static Integer addOrRead(Operation operation,Integer... num) {
		System.out.println("thread:"+Thread.currentThread().getName()+" 进入代码");
		switch(operation) {
			case ADD :
				Arrays.asList(num).stream().forEach((n)->{result += n;});
				break;
			case READ :
		}
		System.out.println("thread:"+Thread.currentThread().getName()+" 退出代码");
		return result;
	}
	
	public enum Operation {
		ADD,READ
	}
	
	/**
	 * 使用Lock的Condition，将主线程阻塞，当新线程完成计算后，唤醒主线程返回计算结果。
	 * @throws InterruptedException
	 */
	public static void useLock() throws InterruptedException {
		Lock lock = new ReentrantLock();
		Condition condition = lock.newCondition();
		new Thread(()->{
			lock.lock();
			try {
				result = 1;
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}).start();
		lock.lock();
		try {
			condition.await();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 与Lock，object.wait() 类似，阻塞主线程，当新线程完成任务后，再唤醒主线程返回计算结果。
	 */
	public static void useLockSupport() {
		Thread thread = Thread.currentThread();
		new Thread(()->{
			result = 1;
			LockSupport.unpark(thread);
		}).start();
		LockSupport.park();
	}

	/**
	 * 使用Thread.join()，主线程只有在新线程完成之后才能返回。
	 * @throws InterruptedException
	 */
	public static void useJoin() throws InterruptedException {
		Thread thread = new Thread(() -> {
			result = 1;
		});
		thread.start();
		thread.join();
	}
	
	/**
	 * 没想出来，与Synchronized类似，无法控制使用信号量的先后顺序。
	 */
	public static void useSemaphore() {
		
	}
	
	/**
	 * 使用CountDownLatch工具类，只有当countDownLatch的数量countDown到0，主线程才可继续运行。
	 * @throws InterruptedException
	 */
	public static void useCountDownLatch() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		new Thread(() -> {
			result = 1;
			countDownLatch.countDown();
		}).start();;
		countDownLatch.await();
	}
	
	/**
	 * 使用CyclicBarrier工具类，当主线程与新线程都await，cyclicBarrier的等待数为2，则唤醒所有线程。
	 * @throws InterruptedException
	 * @throws BrokenBarrierException
	 */
	public static void useCyclicBarrier() throws InterruptedException, BrokenBarrierException {
		CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
		new Thread(() -> {
			result = 1;
			try {
				cyclicBarrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
		}).start();;
		cyclicBarrier.await();
	}
	
	/**
	 * 使用一共享变量表示新线程是否计算完毕，主线程轮训当发现变量表示新线程已计算完毕，则返回。
	 */
	public static void useGlobalVar() {
		new Thread(() -> {
			result = 1;
			finish = true;
		}).start();;
		while(true) {
			if (finish) {
				break;
			}
		}
	}
	

}
