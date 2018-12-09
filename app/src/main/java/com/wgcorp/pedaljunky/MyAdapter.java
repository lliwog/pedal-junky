package com.wgcorp.pedaljunky;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private static final String TAG = "MyAdapter";

    private List<CustomDevice> mDataset = new ArrayList<>();

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_view, parent, false);

        MyAdapter.MyViewHolder viewHolder = new MyAdapter.MyViewHolder(v, new MyViewHolder.IMyViewHolderClicks() {
            public void onDevice(View caller) {
                Log.d(TAG, "You have just clicked on device");
            }
        });
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        CustomDevice btDevice = mDataset.get(position);
        holder.mTextView.setText(btDevice.getName());
        holder.mTextView2.setText(btDevice.getAddress());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * @param device
     */
    public void add(CustomDevice device) {
        CustomDevice foundDevice = mDataset.stream()
                .filter(d -> d.getAddress().equals(device.getAddress())).findAny().orElse(null);

        if (foundDevice == null) {
            mDataset.add(device);
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView mTextView;
        public TextView mTextView2;

        public IMyViewHolderClicks mListener;

        public MyViewHolder(View v, IMyViewHolderClicks listener) {
            super(v);
            mListener = listener;

            mTextView = v.findViewById(R.id.my_text_view);
            mTextView2 = v.findViewById(R.id.my_text_view_2);
        }

        @Override
        public void onClick(View view) {
            mListener.onDevice(view);
        }

        public interface IMyViewHolderClicks {
            void onDevice(View caller);
        }
    }

}
