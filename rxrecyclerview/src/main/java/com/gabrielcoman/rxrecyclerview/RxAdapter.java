package com.gabrielcoman.rxrecyclerview;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action4;
import rx.functions.Func1;

public class RxAdapter extends RecyclerView.Adapter<RxViewHolder> {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private HashMap<Class, Integer> classLayoutMap = new HashMap<>();
    private HashMap<Class, Action4<Integer, View, Object, Integer>> classCustomizeMap = new HashMap<>();
    private HashMap<Class, Action2<Integer, Object>> classClickMap = new HashMap<>();

    private List<Object> data = new ArrayList<>();
    private Action0 didReachEndCallback;
    private RecyclerView recyclerView;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Custom methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static RxAdapter create () {
        return new RxAdapter();
    }

    public RxAdapter bindTo(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return this;
        } else {
            this.recyclerView = recyclerView;
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(this);
            return this;
        }
    }

    public RxAdapter setLayoutManger(RecyclerView.LayoutManager manger) {
        if (recyclerView == null) {
            return this;
        } else {
            recyclerView.setLayoutManager(manger);
            return this;
        }
    }

    public <T> RxAdapter customizeRow (final int rowId, final Class<T> modelClass, final Action4<Integer, View, T, Integer> customise) {
        classLayoutMap.put(modelClass, rowId);
        classCustomizeMap.put(modelClass, new Action4<Integer, View, Object, Integer>() {
            @Override
            public void call(Integer i, View view, Object o, Integer total) {
                customise.call(i, view, (T) o, total);
            }
        });
        return this;
    }

    public <T> RxAdapter didClickOnRow(final Class<T> modelClass, final Action2<Integer, T> click) {
        classClickMap.put(modelClass, new Action2<Integer, Object>() {
            @Override
            public void call(Integer integer, Object o) {
                click.call(integer, (T) o);
            }
        });
        return this;
    }

    public RxAdapter didReachEnd (Action0 customise) {
        didReachEndCallback = customise;
        return this;
    }

    public <T> void update(final List<T> dt) {

        Observable.from(dt)
                .filter(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object item) {
                        Class itemClass = item.getClass();
                        return classLayoutMap.containsKey(itemClass);
                    }
                })
                .toList()
                .subscribe(new Action1<List<T>>() {
                    @Override
                    public void call(List<T> ts) {

                        for (T t: ts) {
                            RxAdapter.this.data.add(t);
                        }

                        notifyDataSetChanged();
                    }
                });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Adapter methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getItemViewType(int position) {
        Object object = data.get(position);
        Class aClass = object.getClass();
        return classLayoutMap.get(aClass);
    }

    @Override
    public RxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View viewHolder = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new RxViewHolder(viewHolder);
    }

    @Override
    public void onBindViewHolder(RxViewHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Object object = data.get(pos);
        Class aClass = object.getClass();
        View viewHolder = holder.itemView;
        Action4<Integer, View, Object, Integer> action = classCustomizeMap.get(aClass);
        if (action != null) {
            action.call(position, viewHolder, object, data.size());
        }

        if (position == data.size() - 1 && didReachEndCallback != null) {
            didReachEndCallback.call();
        }

        final Action2<Integer, Object> click = classClickMap.get(aClass);

        viewHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (click != null) {
                    click.call(pos, object);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
