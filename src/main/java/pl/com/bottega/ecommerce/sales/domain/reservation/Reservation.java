package pl.com.bottega.ecommerce.sales.domain.reservation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pl.com.bottega.ddd.support.domain.BaseAggregateRoot;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.ClientData;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sales.domain.offer.Discount;
import pl.com.bottega.ecommerce.sales.domain.offer.DiscountPolicy;
import pl.com.bottega.ecommerce.sales.domain.offer.Offer;
import pl.com.bottega.ecommerce.sales.domain.offer.OfferItem;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.Product;
import pl.com.bottega.ecommerce.sharedkernel.Money;

public class Reservation extends BaseAggregateRoot {
	public enum ReservationStatus {
		CLOSED, OPENED
	}

	private ClientData clientData;

	private Date createDate;

	private List<ReservationItem> items;

	private ReservationStatus status;

	@SuppressWarnings("unused")
	private Reservation() {
	}

	public Reservation(Id aggregateId, ReservationStatus status, ClientData clientData, Date createDate) {
		this.id = aggregateId;
		this.status = status;
		this.clientData = clientData;
		this.createDate = createDate;
		this.items = new ArrayList<ReservationItem>();
	}

	public void add(Product product, int quantity) {
		if (isClosed()) {
			domainError("Reservation already closed");
		}
		if (!product.isAvailable()) {
			domainError("Product is no longer available");
		}

		if (contains(product)) {
			increase(product, quantity);
		} else {
			addNew(product, quantity);
		}
	}

	private void addNew(Product product, int quantity) {
		final ReservationItem item = new ReservationItem(product, quantity);
		items.add(item);
	}

	private Money calculateItemCost(ReservationItem item) {
		return item.getProduct().getPrice().multiplyBy(item.getQuantity());
	}

	/**
	 * Sample function closured by policy </br>
	 * Higher order function closured by policy function</br>
	 * </br>
	 * Function loads current prices, and prepares offer according to the current
	 * availability and given discount
	 *
	 * @param discountPolicy
	 * @return
	 */
	public Offer calculateOffer(DiscountPolicy discountPolicy) {
		final List<OfferItem> availabeItems = new ArrayList<OfferItem>();
		final List<OfferItem> unavailableItems = new ArrayList<OfferItem>();

		for (final ReservationItem item : items) {
			if (item.getProduct().isAvailable()) {
				final Discount discount = discountPolicy.applyDiscount(item.getProduct(), item.getQuantity(),
						item.getProduct().getPrice());
				final OfferItem offerItem = new OfferItem(item.getProduct().generateSnapshot(), item.getQuantity(),
						discount);

				availabeItems.add(offerItem);
			} else {
				final OfferItem offerItem = new OfferItem(item.getProduct().generateSnapshot(), item.getQuantity());

				unavailableItems.add(offerItem);
			}
		}

		return new Offer(availabeItems, unavailableItems);
	}

	public void close() {
		if (isClosed()) {
			domainError("Reservation is already closed");
		}
		status = ReservationStatus.CLOSED;
	}

	public boolean contains(Product product) {
		for (final ReservationItem item : items) {
			if (item.getProduct().equals(product)) {
				return true;
			}
		}
		return false;
	}

	public ClientData getClientData() {
		return clientData;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public List<ReservedProduct> getReservedProducts() {
		final ArrayList<ReservedProduct> result = new ArrayList<ReservedProduct>(items.size());

		for (final ReservationItem item : items) {
			result.add(new ReservedProduct(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
					calculateItemCost(item)));
		}

		return result;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	private void increase(Product product, int quantity) {
		for (final ReservationItem item : items) {
			if (item.getProduct().equals(product)) {
				item.changeQuantityBy(quantity);
				break;
			}
		}
	}

	public boolean isClosed() {
		return status.equals(ReservationStatus.CLOSED);
	}
}
