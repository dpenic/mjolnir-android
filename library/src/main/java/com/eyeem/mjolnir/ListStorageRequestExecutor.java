package com.eyeem.mjolnir;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eyeem.storage.Storage;

import java.util.HashMap;
import java.util.List;

/**
 * Created by vishna on 02/11/13.
 */
public class ListStorageRequestExecutor {

   public Storage storage;
   public Storage.List list;
   public RequestBuilder requestBuilder;
   public Class objectClass;
   public boolean exhausted;

   public ListStorageRequestExecutor(RequestBuilder requestBuilder, Class objectClass) {
      this.requestBuilder = requestBuilder;
      this.objectClass = objectClass;
   }

   public ListStorageRequestExecutor in(Storage storage) {
      this.storage = storage;
      list = storage.obtainList(String.valueOf(requestBuilder.toUrl().hashCode()));
      list.enableDedupe(true);
      return this;
   }

   public VolleyListRequestExecutor fetchFront(final HashMap<String, String> metaParams) {
      RequestBuilder frontRequest = requestBuilder.copy().fetchFront(list);
      if (metaParams != null) {
         frontRequest.meta.putAll(metaParams);
      }
      return new VolleyListRequestExecutor(frontRequest, objectClass)
         .listener(new Response.Listener<List>() {
            @Override
            public void onResponse(List response) {
               if (metaParams != null && metaParams.containsKey(FORCE_FETCH_FRONT)) {
                  Storage.List transaction = list.transaction();
                  transaction.clear();
                  transaction.addAll(response);
                  exhausted = (transaction.size() == 0);
                  transaction.commit(new Storage.Subscription.Action(Storage.Subscription.ADD_UPFRONT));
               } else {
                  list.addUpFront(response, null);
               }
            }
         })
         .errorListener(new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
               //Toast.makeText(App.the, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
         });
   }

   public VolleyListRequestExecutor fetchBack(HashMap<String, String> metaParams) {
      RequestBuilder backRequest = requestBuilder.copy().fetchBack(list);
      if (metaParams != null) {
         backRequest.meta.putAll(metaParams);
      }
      return new VolleyListRequestExecutor(backRequest, objectClass)
         .listener(new Response.Listener<List>() {
            @Override
            public void onResponse(List response) {
               int before = list.size();
               list.addAll(response);
               exhausted = (before == list.size());
            }
         })
         .errorListener(new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
               //Toast.makeText(App.the, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
         });
   }

   public final static String FORCE_FETCH_FRONT = "forceFetchFront";

   public static HashMap<String, String> forceFrontFetch() {
      HashMap<String, String> params = new HashMap<String, String>();
      params.put(FORCE_FETCH_FRONT, "true");
      return params;
   }
}
