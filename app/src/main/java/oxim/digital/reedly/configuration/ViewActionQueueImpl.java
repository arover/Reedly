package oxim.digital.reedly.configuration;

import java.util.Iterator;
import java.util.LinkedList;

import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public final class ViewActionQueueImpl<View> implements ViewActionQueue<View> {

    private final LinkedList<Action1<View>> viewActions = new LinkedList<>();
    private final Object queueLock = new Object();

    private final PublishSubject<Action1<View>> viewActionSubject = PublishSubject.create();
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    private final Scheduler observeScheduler;

    private boolean isPaused = true;

    public ViewActionQueueImpl(final Scheduler observeScheduler) {
        this.observeScheduler = observeScheduler;
    }

    @Override
    public void subscribeTo(final Observable<Action1<View>> observable,
                            final Action1<View> onCompleteAction,
                            final Action1<Throwable> errorAction) {

        subscriptions.add(observable.observeOn(observeScheduler)
                .subscribe(this::onResult, errorAction, () -> onResult(onCompleteAction)));
    }

    @Override
    public void subscribeTo(final Single<Action1<View>> single,
                            final Action1<Throwable> errorAction) {

        subscriptions.add(single.observeOn(observeScheduler)
                .subscribe(this::onResult, errorAction));

    }

    @Override
    public void subscribeTo(final Completable completable, final Action1<View> onCompleteAction,
                            final Action1<Throwable> errorAction) {

        subscriptions.add(completable.observeOn(observeScheduler)
                .subscribe(() -> onResult(onCompleteAction), errorAction));

    }

    private void onResult(final Action1<View> resultAction) {
        if (isPaused) {
            synchronized (queueLock) {
                viewActions.add(resultAction);
            }
        } else {
            viewActionSubject.onNext(resultAction);
        }
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
        isPaused = false;
        consumeQueue();
    }

    @Override
    public void destroy() {
        subscriptions.unsubscribe();
        viewActionSubject.onCompleted();
    }

    private void consumeQueue() {
        synchronized (queueLock) {
            final Iterator<Action1<View>> actionIterator = viewActions.iterator();
            while (actionIterator.hasNext()) {
                final Action1<View> pendingViewAction = actionIterator.next();
                viewActionSubject.onNext(pendingViewAction);
                actionIterator.remove();
            }
        }
    }

    @Override
    public Observable<Action1<View>> viewActionsObservable() {
        return viewActionSubject;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ViewActionQueueImpl<?> that = (ViewActionQueueImpl<?>) o;

        if (isPaused != that.isPaused) {
            return false;
        }
        if (!viewActions.equals(that.viewActions)) {
            return false;
        }
        if (!queueLock.equals(that.queueLock)) {
            return false;
        }
        if (!viewActionSubject.equals(that.viewActionSubject)) {
            return false;
        }
        if (!subscriptions.equals(that.subscriptions)) {
            return false;
        }
        return observeScheduler != null ? observeScheduler
                .equals(that.observeScheduler) : that.observeScheduler == null;
    }

    @Override
    public int hashCode() {
        int result = viewActions.hashCode();
        result = 31 * result + queueLock.hashCode();
        result = 31 * result + viewActionSubject.hashCode();
        result = 31 * result + subscriptions.hashCode();
        result = 31 * result + (observeScheduler != null ? observeScheduler.hashCode() : 0);
        result = 31 * result + (isPaused ? 1 : 0);
        return result;
    }
}
