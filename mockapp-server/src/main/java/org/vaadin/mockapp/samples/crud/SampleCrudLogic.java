package org.vaadin.mockapp.samples.crud;

import org.vaadin.mockapp.MockAppUI;
import org.vaadin.mockapp.samples.backend.DataService;
import org.vaadin.mockapp.samples.data.Product;
import org.vaadin.teemu.clara.binder.annotation.UiHandler;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroup.CommitHandler;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.Page;
import com.vaadin.ui.Label;

public class SampleCrudLogic {

	private SampleCrudView view;
	private BeanFieldGroup<Product> fieldGroup;

	public SampleCrudLogic(SampleCrudView simpleCrudView) {
		this.view = simpleCrudView;
	}

	public void init() {
		setupTable();
		setupForm();

		// Hide and disable if not admin
		if (!MockAppUI.get().getAccessControl().isUserInRole("admin")) {
			view.layout.replaceComponent(view.form, new Label(
					"Login as 'admin' to have edit access"));
			view.newProduct.setEnabled(false);
		}

		refreshTable();
	}

	private void setupForm() {
		fieldGroup = new BeanFieldGroup<Product>(Product.class);
		fieldGroup.bindMemberFields(view.form);

		fieldGroup.addCommitHandler(new CommitHandler() {

			@Override
			public void preCommit(CommitEvent commitEvent)
					throws CommitException {
			}

			@Override
			public void postCommit(CommitEvent commitEvent)
					throws CommitException {
				DataService.get().updateProduct(
						fieldGroup.getItemDataSource().getBean());
			}
		});

		// Set default field values
		setFormDataSource(null);

	}

	public void discardProduct() {
		fieldGroup.discard();
		setFragmentParameter("");
		view.table.setValue(null);
		hideFormPopup();
	}

	/**
	 * Update the fragment without causing navigator to change view
	 */
	private void setFragmentParameter(String productId) {
		String fragmentParameter;
		if (productId == null || productId.isEmpty()) {
			fragmentParameter = "";
		} else {
			fragmentParameter = productId;
		}

		Page page = view.getUI().getPage();
		page.setUriFragment("!" + SampleCrudView.VIEW_NAME + "/"
				+ fragmentParameter, false);
	}

	public void enter(String productId) {
		if (productId != null && !productId.isEmpty()) {
			if (productId.equals("new"))
				newProduct();
			else {
				// Ensure this is selected even if coming directly here from
				// login
				try {
					int pid = Integer.parseInt(productId);
					view.table.setValue(pid);
					editProduct(pid);
				} catch (NumberFormatException e) {
				}
			}
		}
	}

	protected void editProduct(Integer productId) {
		Product product = null;
		if (productId == null)
			setFragmentParameter("");
		else {
			setFragmentParameter(productId + "");
			product = findProduct(productId);
		}
		setFormDataSource(product);

		if (product != null)
			showFormPopup();
	}

	private Product findProduct(int productId) {
		return DataService.get().getProductById(productId);
	}

	public void saveProduct() {
		try {
			fieldGroup.commit();
			Product p = fieldGroup.getItemDataSource().getBean();
			view.showSaveNotification(p.getProductName() + " (" + p.getId()
					+ ") updated");
			view.table.setValue(null);
			refreshTable();
			setFragmentParameter("");
			hideFormPopup();
		} catch (CommitException e) {
			view.showError("Please re-check the fields");
			e.printStackTrace();
		}
	}

	public void deleteProduct() {
		try {
			Product p = fieldGroup.getItemDataSource().getBean();
			DataService.get().deleteProduct(p.getId());
			view.showSaveNotification(p.getProductName() + " (" + p.getId()
					+ ") removed");

			view.table.setValue(null);
			refreshTable();
			setFragmentParameter("");
			hideFormPopup();
		} catch (Exception e) {

		}

	}

	private void setupTable() {
		view.table.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				editProduct(view.table.getValue());
			}
		});
	}

	public void setFormDataSource(Product product) {
		view.form.setEnabled(product != null);
		if (product == null) {
			fieldGroup.setItemDataSource(new BeanItem<Product>(new Product()));
		} else {
			fieldGroup.setItemDataSource(new BeanItem<Product>(product));
		}
	}

	private void refreshTable() {
		Object oldSelection = view.table.getValue();
		BeanContainer<Integer, Product> container = view.table
				.getContainerDataSource();
		container.removeAllItems();
		container.addAll(DataService.get().getAllProducts());
		view.table.setValue(oldSelection);
	}

	public void newProduct() {
		view.table.setValue(null);
		setFragmentParameter("new");
		setFormDataSource(new Product());
		showFormPopup();
	}

	private void showFormPopup() {
		if (view.formAsPopup.getValue()) {
			view.getUI().addWindow(view.formWindow);
			view.formWindow.center();
		}
	}

	private void hideFormPopup() {
		if (view.formWindow.isAttached()) {
			view.formWindow.close();
		}

	}

}
