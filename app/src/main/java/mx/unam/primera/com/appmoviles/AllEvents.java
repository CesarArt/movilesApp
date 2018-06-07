package mx.unam.primera.com.appmoviles;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mx.unam.primera.com.adapter.EventAdapter;
import mx.unam.primera.com.logic.Service;
import mx.unam.primera.com.model.Event;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AllEvents.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AllEvents#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AllEvents extends Fragment implements SwipeRefreshLayout.OnRefreshListener
{
    //recicler para listas

    private RecyclerView recycler;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager IManager;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private int eventType;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    List<Event> events;
    Service service;
    ProgressBar pb;
    SwipeRefreshLayout srlRefresh;

    Thread tr;

    public AllEvents() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AllEvents.
     */
    // TODO: Rename and change types and number of parameters
    public static AllEvents newInstance(String param1, String param2) {
        AllEvents fragment = new AllEvents();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        service = new Service();
        events = new ArrayList<>();

        eventType = Integer.parseInt(String.valueOf(
                getArguments().getSerializable("EventType")
        ));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_all_events, container, false);
        getActivity().setTitle(getTitle());

        pb = (ProgressBar)view.findViewById(R.id.pbLoading);
        recycler =(RecyclerView) view.findViewById(R.id.reciclador);

        IManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recycler.setLayoutManager(IManager);

        tr = setLoadingThread();
        tr.start();

        srlRefresh = (SwipeRefreshLayout)view.findViewById(R.id.srlRefresh);
        srlRefresh.setOnRefreshListener(this);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if(tr.isAlive())
        {
            try
            {
                tr.wait();
                Log.w("Tr join", "Se espero al hilo " + tr.getName());
            } catch (Exception ex)
            {
                Log.d("Tr wait", "Hilo interrumpido");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh()
    {
        tr = setLoadingThread();
        tr.start();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private Thread setLoadingThread()
    {
        Thread tr = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    events = service.getData(getActivity().getApplicationContext(), null, eventType);
                    Log.d("Eventos encontrados", String.valueOf(events.size()));
                }
                catch (Exception ex)
                {
                    Log.d("Thread tr", "Ha ocurrido un error al intentar cargar los datos");
                    Log.e("Error Thread", ex.getMessage());
                }
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        pb.setVisibility(View.GONE);
                        srlRefresh.setRefreshing(false);
                        try
                        {
                            if(events != null)
                            {
                                if(events.size() == 0)
                                {
                                    Toast.makeText(getActivity().getApplicationContext(),
                                        "No hay eventos resgistrados", Toast.LENGTH_SHORT).show();
                                }
                                adapter = new EventAdapter(events);
                                recycler.setAdapter(adapter);
                            }
                            else
                            {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "No se encontraron datos", Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception ex)
                        {
                            String msg = "";
                            if(events == null)
                                msg = "Ha habido un problema. Verifica tu conexión a internet";
                            else
                                msg = "Ha habido un problema";

                            Toast.makeText(getActivity().getApplicationContext(),
                                    msg, Toast.LENGTH_LONG).show();
                            Log.e("Mensaje de error", ex.getMessage());
                        }
                    }
                });
            }
        };
        return tr;
    }

    private String getTitle()
    {
        String title = "Guia";
        switch (eventType)
        {
            case -1:
                title = "Mostrar todo";
                break;
            case 1:
                title = "Futbol americano";
                break;
            case 2:
                title = "Futbol soccer";
                break;
            case 3:
                title = "Basquetbol";
                break;
            case 4:
                title = "Baseball";
                break;
            case 5:
                title = "Conciertos y musicales";
                break;
            case 6:
                title = "Premios";
                break;
            default:
                title = "Otros";
                break;
        }
        return title;
    }
}
