package com.holger.mqttliga;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GameListAdapter extends RecyclerView.Adapter<GameListHolder> {

	private final Events eventsList;
	private final int itemResource;

	GameListAdapter(int itemResource, Events eventsList) {
		this.eventsList = eventsList;
		this.itemResource = itemResource;
	}

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

	@Override
	public GameListHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(this.itemResource, parent, false);
		return new GameListHolder(view, this);
	}

	@Override
	public void onBindViewHolder(GameListHolder holder, int position) {

		Events eventsList = this.eventsList;
		holder.bindGameList(eventsList,position);
	}

	@Override
	public int getItemCount() {

		return this.eventsList.getCount();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return position;
	}
}
