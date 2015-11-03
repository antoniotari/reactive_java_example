package com.antoniotari.rxjavastudy;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by antonio on 2015-10-24.
 */
public class RxMain implements Executor {

    private BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public static void main(String... args) throws InterruptedException {
        RxMain rxMain = new RxMain();
        rxMain.runProgram();
        rxMain.runLoop();
    }


    private void runLoop() throws InterruptedException {
        while(!Thread.interrupted()){
            tasks.take().run();
        }
    }

    public void runProgram(){
        ConnectableObservable<String> obs1 = Observable.create(new OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                try {
                    Thread.sleep(1000);
                    subscriber.onNext("uno");
                    System.out.println("finished 1");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(this))
                .publish();

        ConnectableObservable<String> obs2 = Observable.create(new OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                try {
                    Thread.sleep(7000);
                    subscriber.onNext("due");
                    System.out.println("finished 2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(this))
                .publish();

        Observable.combineLatest(obs1, obs2, (x, y) -> x + " " + y)
                .subscribeOn(Schedulers.io())
                .subscribe(sum -> System.out.println("update " + sum),
                        error -> {
                            System.out.println("Got an error!");
                            error.printStackTrace();
                        }, this::closeProgram);

        obs1.connect(new Action1<Subscription>() {
            @Override
            public void call(final Subscription subscription) {
                System.out.println("connect 1 callback");
            }
        });
        obs2.connect();
    }

    private void closeProgram(){
        System.out.println("Exiting...");
        Thread.currentThread().interrupt();
    }

    @Override
    public void execute(final Runnable command) {
        tasks.add(command);
    }
}
