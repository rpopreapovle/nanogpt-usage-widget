        v.setTextViewText(R.id.w_balance, UsageRepo.balanceText(context));
        v.setTextViewText(R.id.w_today, "сегодня: " + UsageRepo.todayText(context));
        v.setTextViewText(R.id.w_requests, "запросов: " + UsageRepo.requestsText(context));
        v.setTextViewText(R.id.w_updated, hasKey ? UsageRepo.updatedText(context) : "откройте приложение и вставьте ключ");
