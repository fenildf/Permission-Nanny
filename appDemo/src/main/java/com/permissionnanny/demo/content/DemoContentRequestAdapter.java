package com.permissionnanny.demo.content;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.permissionnanny.common.BundleUtil;
import com.permissionnanny.demo.ContentRequestFactory;
import com.permissionnanny.demo.DemoViewHolder;
import com.permissionnanny.demo.R;
import com.permissionnanny.lib.Nanny;
import com.permissionnanny.lib.request.content.ContentListener;

/**
 *
 */
public class DemoContentRequestAdapter extends RecyclerView.Adapter<DemoViewHolder> {

    private ContentRequestFactory mFactory;
    private Bundle[] mResults;
    private String[] mContent;

    public DemoContentRequestAdapter(ContentRequestFactory factory) {
        mFactory = factory;
        mResults = new Bundle[mFactory.getCount()];
        mContent = new String[mFactory.getCount()];
    }

    @Override
    public int getItemCount() {
        return mFactory.getCount();
    }

    @Override
    public DemoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DemoViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.wifi_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(DemoViewHolder holder, final int position) {
        holder.tvRequest.setText(mFactory.getLabel(position));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFactory.getRequest(position).listener(new ContentListener() {
                    @Override
                    public void onResponse(@NonNull Bundle response, @Nullable Cursor data) {
                        mResults[position] = response;
                        mContent[position] = DatabaseUtils.dumpCursorToString(data);
                        notifyItemChanged(position);
                    }
                }).startRequest(v.getContext(), "demo");
            }
        });

        Bundle results = mResults[position];
        if (results == null) {
            holder.tvResponse.setText(null);
            holder.itemView.setBackgroundColor(0);
        } else if (Nanny.SC_OK == results.getInt(Nanny.STATUS_CODE)) {
            holder.tvResponse.setText("Allowed\n" + BundleUtil.toString(results) + '\n' + mContent[position]);
            holder.itemView.setBackgroundColor(0xFF00FF00);
        } else {
            holder.tvResponse.setText("Denied");
            holder.itemView.setBackgroundColor(0xFFFF0000);
        }
    }
}
