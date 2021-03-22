package com.mycommonlibrary.frame.RxBus;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle2.LifecycleProvider;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.List;

public class RxBus {
    private volatile static RxBus instance;
    private final Subject<Object> mBus;

    private RxBus() {
        mBus = PublishSubject.create().toSerialized();
    }

    public static RxBus getInst() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) {
                    instance = new RxBus();
                }
            }
        }
        return instance;
    }

    /**
     * 发送事件
     * 将数据添加到订阅
     * 这个地方是再添加订阅的地方。最好创建一个新的类用于数据的传递
     */
    public static void post(@NonNull Object o) {
        if (RxBus.getInst().hasObservers()) {//判断当前是否已经添加订阅
            RxBus.getInst().mBus.onNext(o);
        }
    }

    /**
     * 这个是传递集合如果有需要的话你也可以进行更改
     */
    public static <T> void postList(@NonNull List<T> obj) {
        if (RxBus.getInst().hasObservers()) {//判断当前是否已经添加订阅
            RxBus.getInst().mBus.onNext(obj);
        }
    }

    /**
     * 使用Rxlifecycle解决RxJava引起的内存泄漏
     * bindToLifecycle()方法，完成Observable发布的事件和当前的组件绑定，
     * 实现生命周期同步。从而实现当前组件生命周期结束时，自动取消对Observable订阅
     */
    public <T> Observable<T> toObservable(LifecycleOwner owner, final Class<T> eventType) {
        LifecycleProvider<Lifecycle.Event> provider = AndroidLifecycle.createLifecycleProvider(owner);
        return mBus.ofType(eventType)
                .subscribeOn(Schedulers.io())// 在子线程订阅事件
                .observeOn(AndroidSchedulers.mainThread())// 在主线程接收订阅的事件
                .compose(provider.<T>bindToLifecycle());
    }

    /**
     * 注册一条接收事件，接收者必须是具有生命周期的UI组件（Activty或Fragment）
     *
     * @param owner     Activity或Fragment的实例
     * @param eventType 需要接收的数据类名称
     */
    public static <T> Observable<T> register(LifecycleOwner owner, final Class<T> eventType) {
        return RxBus.getInst()
                .toObservable(owner, eventType);
    }

    /**
     * 注册一条接收事件，接收者必须是具有生命周期的UI组件（Activty或Fragment）
     *
     * @param owner     Activity或Fragment的实例（主要是防止内存泄露）
     * @param eventType 需要接收的数据类名称
     */
    public static <T> Disposable register(LifecycleOwner owner, final Class<T> eventType, Consumer<T> consumer) {
        return RxBus.getInst()
                .toObservable(owner, eventType)
                .subscribe(consumer);
    }

    /**
     * 判断是否有订阅者
     */
    public boolean hasObservers() {
        return mBus.hasObservers();
    }

}
