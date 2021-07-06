package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAccountBinding;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.ui.activity.AccountManagerActivity;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.model.AccountViewModel;

public class AccountFragment extends AbstractAccountManagerFragment {

    private FragmentAccountBinding binding;

    private AccountViewModel accountViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final AccountFragmentArgs arguments = AccountFragmentArgs.fromBundle(requireArguments());
        final long accountId = arguments.getId();

        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);

        final ViewModelProvider viewModelProvider = new ViewModelProvider(
                getViewModelStore(),
                new AccountViewModel.Factory(
                        requireActivity().getApplication(),
                        accountId
                )
        );
        this.accountViewModel = viewModelProvider.get(AccountViewModel.class);

        this.accountViewModel.getAccountName().observe(getViewLifecycleOwner(), this::onAccountName);

        this.binding.remove.setOnClickListener(this::onRemoveAccount);
        this.binding.identities.setOnClickListener(this::onIdentities);
        this.binding.labels.setOnClickListener(this::onLabels);
        this.binding.vacationResponse.setOnClickListener(this::onVacationResponse);
        this.binding.e2ee.setOnClickListener(this::onE2ee);

        return this.binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_account, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.launch) {
            LttrsActivity.launch(getActivity(), accountViewModel.getAccountId());
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void onAccountName(final AccountName account) {
        if (account != null) {
            this.binding.setAccount(account);
        } else {
            AccountManagerActivity.relaunch(requireActivity());
        }
    }

    private void onE2ee(final View view) {

    }

    private void onVacationResponse(final View view) {

    }

    private void onLabels(final View view) {

    }

    private void onIdentities(final View view) {

    }

    private void onRemoveAccount(final View view) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.remove_account_dialog_title)
                .setMessage(R.string.remove_account_dialog_message)
                .setPositiveButton(
                        R.string.remove_account,
                        (dialog, which) -> accountViewModel.removeAccount()
                )
                .setNegativeButton(R.string.cancel, null)
                .show();

    }


}
