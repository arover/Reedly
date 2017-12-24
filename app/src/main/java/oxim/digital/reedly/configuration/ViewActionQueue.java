package oxim.digital.reedly.configuration;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Action1;

public interface ViewActionQueue<View> {

    /**
     * @param observable observable ,a UseCase that emmit multi result.
     * @param onCompleteAction action when observable complete.
     * @param errorAction error handler
     */
    void subscribeTo(Observable<Action1<View>> observable, Action1<View> onCompleteAction,
                     Action1<Throwable> errorAction);

    /**
     * @param single      a Single observable
     * @param errorAction error action
     */
    void subscribeTo(Single<Action1<View>> single, Action1<Throwable> errorAction);

    /**
     * @param completable a UseCase which emmit no result, may throw error.
     * @param onCompleteAction on complete action
     * @param errorAction error handler
     */
    void subscribeTo(Completable completable, Action1<View> onCompleteAction,
                     Action1<Throwable> errorAction);

    void pause();

    void resume();

    void destroy();

    Observable<Action1<View>> viewActionsObservable();
}
