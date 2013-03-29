/*
 * Copyright (c) 2013 Kinvey Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kinvey.sample.ticketview;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;

import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.ArrayList;
import java.util.List;

import com.kinvey.sample.ticketview.model.Ticket;

/**
 * @author mjsalinger
 * @since 2.0
 */
public class TicketListFragment extends SherlockListFragment {
    private ListView listView;
    private TicketAdapter myAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        myAdapter = new TicketAdapter(getActivity(), ((TicketViewActivity) getActivity()).getTicketList(),
                (LayoutInflater) getActivity().getSystemService(
                        SherlockFragmentActivity.LAYOUT_INFLATER_SERVICE));
        setListAdapter(myAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews();
    }

    private void bindViews() {
        listView = getListView();
        setListeners();
    }

    private void setListeners() {
        // Create a ListView-specific touch listener. ListViews are given special treatment because
        // by default they handle touches for their list items... i.e. they're in charge of drawing
        // the pressed state (the list selector), handling list item clicks, etc.
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.OnDismissCallback() {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {

                                    myAdapter.remove(myAdapter.getItem(position));
                                }
                                myAdapter.notifyDataSetChanged();
                            }
                        });
        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((TicketViewActivity) getActivity()).ticketDetailsFragment(position);
    }

    private class TicketAdapter extends ArrayAdapter<Ticket> {

        private LayoutInflater mInflater;

        public TicketAdapter(Context context, List<Ticket> tickets,
                             LayoutInflater inf) {
            // NOTE: I pass an arbitrary textViewResourceID to the super
            // constructor-- Below I override
            // getView(...), which causes the underlying adapter to ignore this
            // field anyways, it is just needed in the constructor.
            super(context, R.layout.ticket_list, tickets);
            this.mInflater = inf;

        }

        @Override
        public void remove(Ticket object) {
            object.setStatus("closed");
            ((TicketViewActivity) getActivity()).saveTicket(object);
            super.remove(object);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TicketViewHolder holder = null;

            TextView name = null;
            TextView description = null;
            TextView requestedBy = null;
            ImageButton commentButton;

            Ticket rowData = getItem(position);

            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.ticket_list, null);
                holder = new TicketViewHolder(convertView);
                convertView.setTag(holder);
            }
            holder = (TicketViewHolder) convertView.getTag();

            name = holder.getName();
            name.setText(rowData.getSubject());
            description = holder.getDescription();
            description.setText(rowData.getDescription().length() > 70 ?
                    rowData.getDescription().substring(0,69) + " ..." : rowData.getDescription());
            commentButton = holder.getImageButton();
            commentButton.setFocusable(false);
            commentButton.setFocusableInTouchMode(false);
            commentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int position = getListView().getPositionForView((LinearLayout)view.getParent());
                    if (position >= 0) {
                        ((TicketViewActivity) getActivity()).showCommentDialog(position);
                    }
                }
            });


            return convertView;
        }

        /**
         * This pattern is used as an optimization for Android ListViews.
         *
         * Since every row uses the same layout, the View object itself can be
         * recycled, only the data/content of the row has to be updated.
         *
         * This allows for Android to only inflate enough Row Views to fit on
         * screen, and then they are recycled. This allows us to avoid creating
         * a new view for every single row, which can have a negative effect on
         * performance (especially with large lists on large screen devices).
         *
         */
        private class TicketViewHolder {
            private View mRow;

            private TextView tvName = null;
            private TextView tvDescription = null;
            private ImageButton commentButton = null;

            public TicketViewHolder(View row) {
                mRow = row;
            }

            public TextView getName() {
                if (tvName == null) {
                    tvName = (TextView) mRow.findViewById(R.id.ticketName);
                }
                return tvName;
            }

            public TextView getDescription() {
                if (tvDescription == null) {
                    tvDescription = (TextView) mRow.findViewById(R.id.ticketDescription);
                }
                return tvDescription;
            }

            public ImageButton getImageButton() {
                if (commentButton == null) {
                    commentButton = (ImageButton) mRow.findViewById(R.id.commentButton);
                }
                return commentButton;
            }

        }
    }
}