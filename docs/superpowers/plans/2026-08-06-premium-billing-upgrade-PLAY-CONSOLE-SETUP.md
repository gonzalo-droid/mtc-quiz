# Play Console setup needed before this branch can be tested end-to-end

The code now queries **two** subscription products. Create both in Play Console →
Monetize → Products → Subscriptions, on the same app listing used for `mtcquiz_premium_annual`
today:

1. `mtcquiz_premium_monthly` — base plan billed monthly. Suggested price: S/ 9.90/mes.
2. `mtcquiz_premium_annual` — base plan billed yearly (this ID must stay exactly as-is;
   it's already referenced in the code). Suggested price: S/ 29.90/año.

Both must be type "Subscription" (`SUBS`), status "Active", with at least one base plan
and offer published — `PremiumRepositoryImpl.loadAvailablePlans()` reads the first offer's
first pricing phase (`subscriptionOfferDetails.first().pricingPhases.pricingPhaseList.first()`)
to get the price shown in the UI, so each product needs exactly one straightforward
recurring offer (no free trial/intro price needed for this to work, but if you add one,
confirm the first pricing phase is still the recurring one you want displayed — an intro
price phase would show first instead of the regular price).

Nothing else in the code needs to change once these two products are live — `PremiumScreen`
pulls price and period directly from what Play Console returns.
