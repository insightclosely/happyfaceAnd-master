package com.happyface.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.happyface.R;
import com.happyface.activities.LogInActivity;
import com.happyface.activities.MapsActivity;
import com.happyface.activities.SplashActivity;
import com.happyface.helpers.CallbackRetrofit;
import com.happyface.helpers.PrefManager;
import com.happyface.helpers.RetrofitModel;
import com.happyface.helpers.StaticMembers;
import com.happyface.models.area_models.AreaResponse;
import com.happyface.models.area_models.DataItem;
import com.happyface.models.login_models.EditNameResponse;
import com.happyface.models.login_models.User;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static com.happyface.helpers.StaticMembers.AREA;
import static com.happyface.helpers.StaticMembers.LAT_;
import static com.happyface.helpers.StaticMembers.LON;
import static com.happyface.helpers.StaticMembers.USER;
import static com.happyface.helpers.StaticMembers.openLogin;

public class AccountFragment extends Fragment {

    private String selectedArea;
    private double slat, slong;

    public static AccountFragment getInstance() {
        return new AccountFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_account, container, false);
        return v;
    }

    @BindView(R.id.progress)
    RelativeLayout progress;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.phone)
    TextView phone;
    @BindView(R.id.email)
    TextView email;
    @BindView(R.id.gov)
    TextView gov;
    @BindView(R.id.area)
    AppCompatSpinner area;
    @BindView(R.id.block)
    TextView block;
    @BindView(R.id.street)
    TextView street;
    @BindView(R.id.avenue)
    TextView avenue;
    @BindView(R.id.remarkAddress)
    TextView remarkAddress;
    @BindView(R.id.houseNo)
    TextView houseNo;
    @BindView(R.id.changeLang)
    CardView changeLang;
    @BindView(R.id.currentLocation)
    Button currentLocation;
    @BindView(R.id.signOut)
    CardView signOut;
    @BindView(R.id.saveArea)
    CardView saveArea;

    private User user;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        updateUI();
        changeLang.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            alertDialog.setTitle(R.string.change_language);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()), android.R.layout.simple_list_item_1);
            adapter.add("English");
            adapter.add("العربية");
            alertDialog.setAdapter(adapter,
                    (dialog, which) -> {
                        if (adapter.getItem(which) != null)
                            if (adapter.getItem(which).equals("English") && !StaticMembers.getLanguage(getContext()).equals("en")) {
                                StaticMembers.changeLocale(getContext(), "en");
                                StaticMembers.startActivityOverAll(getActivity(), SplashActivity.class);
                            } else if (!StaticMembers.getLanguage(getContext()).equals("ar")) {
                                StaticMembers.changeLocale(getContext(), "ar");
                                StaticMembers.startActivityOverAll(getActivity(), SplashActivity.class);
                            }

                    });

            alertDialog.show();
        });
        signOut.setOnClickListener(v -> {
            PrefManager.getInstance(getContext()).setAPIToken("");
            PrefManager.getInstance(getContext()).setObject(USER, null);
            StaticMembers.startActivityOverAll(getActivity(), LogInActivity.class);
        });
        currentLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapsActivity.class);
            intent.putExtra(StaticMembers.LAT, slat);
            intent.putExtra(StaticMembers.LONG, slong);
            startActivityForResult(intent, StaticMembers.LOCATION_CODE);
        });
        saveArea.setOnClickListener(v -> {
            changeField(AREA, "", selectedArea);
            saveArea.setVisibility(View.GONE);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == StaticMembers.LOCATION_CODE && data != null) {
                changeField(LAT_, slat + "", "" + data.getDoubleExtra(StaticMembers.LAT, 0));
                changeField(LON, slong + "", "" + data.getDoubleExtra(StaticMembers.LONG, 0));
                slat = data.getDoubleExtra(StaticMembers.LAT, 0);
                slong = data.getDoubleExtra(StaticMembers.LONG, 0);
                currentLocation.setText(String.format(Locale.getDefault(), "%f, %f", slat, slong));
            }
        }
    }

    private void getAreas(List<String> list, ArrayAdapter<String> adapter) {
        AreaResponse areaResponse = (AreaResponse) PrefManager.getInstance(getActivity()).getObject(StaticMembers.AREA, AreaResponse.class);
        int selectedPos = 0;
        if (areaResponse != null) {
            list.clear();
            for (int i = 0; i < areaResponse.getData().size(); i++) {
                DataItem dataItem = areaResponse.getData().get(i);
                if (dataItem.getName() != null) {
                    list.add(dataItem.getName());
                    if (dataItem.getName().equals(user.getArea()))
                        selectedPos = i;
                }
            }
            adapter.notifyDataSetChanged();
            area.setSelection(selectedPos);
        }
        Call<AreaResponse> call = RetrofitModel.getApi(getActivity()).getAreas();
        call.enqueue(new CallbackRetrofit<AreaResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<AreaResponse> call, @NotNull Response<AreaResponse> response) {
                progress.setVisibility(View.GONE);
                int selectedPos = 0;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    list.clear();
                    for (int i = 0; i < response.body().getData().size(); i++) {
                        DataItem dataItem = response.body().getData().get(i);
                        if (dataItem.getName() != null) {
                            list.add(dataItem.getName());
                            if (dataItem.getName().equals(user.getArea()))
                                selectedPos = i;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    area.setSelection(selectedPos);
                    PrefManager.getInstance(getContext()).setObject(StaticMembers.AREA, response.body());
                }
            }

            @Override
            public void onFailure(@NotNull Call<AreaResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                progress.setVisibility(View.GONE);
            }
        });
    }


    void openDialog(String key, String nameDisplay, String defaultVal) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        final EditText input = new EditText(getActivity());
        alertDialog.setTitle(String.format(Locale.getDefault(), getString(R.string.edit_s), nameDisplay));
        input.setHint(key);
        input.setText(defaultVal);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input); // uncomment this line
        alertDialog.setPositiveButton(getString(R.string.ok), (dialog, which) -> changeField(key, defaultVal, input.getText().toString()));
        alertDialog.setNegativeButton(getString(R.string.cancel), null);
        alertDialog.show();
    }

    void updateUI() {
        user = (User) PrefManager.getInstance(getContext()).getObject(USER, User.class);
        if (user == null) {
            openLogin(getContext());
        } else {
            phone.setText(user.getTelephone());
            email.setText(user.getEmail());
            gov.setText(user.getGovernmant());
            //area.setText(user.getArea());
            slat = Double.parseDouble(user.getLat());
            slong = Double.parseDouble(user.getLon());
            currentLocation.setText(String.format(Locale.getDefault(), "%f, %f", slat, slong));
            block.setText(user.getBlock());
            street.setText(user.getStreet());
            avenue.setText(user.getAvenue());
            name.setText(user.getName());
            name.setOnClickListener(v -> openDialog(StaticMembers.NAME, getString(R.string.name), user.getName()));
            gov.setOnClickListener(v -> openDialog(StaticMembers.GOV, getString(R.string.governorate), user.getGovernmant()));
            //area.setOnClickListener(v -> openDialog(StaticMembers.AREA, getString(R.string.area), user.getArea()));
            block.setOnClickListener(v -> openDialog(StaticMembers.BLOCK, getString(R.string.block), user.getBlock()));
            street.setOnClickListener(v -> openDialog(StaticMembers.STREET, getString(R.string.street), user.getStreet()));
            avenue.setOnClickListener(v -> openDialog(StaticMembers.AVENUE, getString(R.string.avenue), user.getAvenue()));
            remarkAddress.setOnClickListener(v -> openDialog(StaticMembers.REMARK_ADDRESS, getString(R.string.remark_address), user.getRemarkaddress()));
            houseNo.setOnClickListener(v -> openDialog(StaticMembers.HOUSE_NO, getString(R.string.house_no), user.getHouse_no()));
            List<String> areas = new ArrayList<>();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, areas);
            area.setAdapter(adapter);
            getAreas(areas, adapter);
            area.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedArea = areas.get(position);
                    saveArea.setVisibility(View.VISIBLE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    void changeField(String key, String defaultVal, String s) {
        if (defaultVal != null)
            if (defaultVal.equals(s))
                return;

        progress.setVisibility(View.VISIBLE);
        HashMap<String, String> params = new HashMap<>();
        params.put(key, s);
        Call<EditNameResponse> call = RetrofitModel.getApi(getContext()).editField(params);
        call.enqueue(new CallbackRetrofit<EditNameResponse>(getContext()) {
            @Override
            public void onResponse(@NotNull Call<EditNameResponse> call, @NotNull Response<EditNameResponse> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    EditNameResponse result = response.body();
                    if (result != null) {
                        if (result.isStatus()) {
                            PrefManager.getInstance(getContext()).setAPIToken(result.getData().getToken());
                            PrefManager.getInstance(getContext()).setObject(StaticMembers.USER, result.getData().getUser());
                            updateUI();
                            saveArea.setVisibility(View.GONE);
                        }
                        StaticMembers.toastMessageShort(getContext(), result.getMessage());
                    }
                } else {
                    StaticMembers.checkLoginRequired(response.errorBody(), getContext());
                }
            }

            @Override
            public void onFailure(@NotNull Call<EditNameResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                progress.setVisibility(View.GONE);
            }
        });
    }
}
