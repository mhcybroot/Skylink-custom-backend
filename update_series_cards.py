import re

with open('/home/mhcybroot/Projects/Skylink-custom-backend/src/main/resources/templates/admin-analyst-dashboard.html', 'r') as f:
    content = f.read()

for series in ['100', '200', '300', '400', '500', '600', '700', '800', '900', 'Others']:
    old_block = f"""                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">Bid</span>
                                                <span class="text-secondary fw-medium" th:text="${{'$' + (entry.series{series}BidAmount != null ? #numbers.formatDecimal(entry.series{series}BidAmount, 1, 'COMMA', 2, 'POINT') : '0.00')}}"></span>
                                            </div>
                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">Inv</span>
                                                <span class="text-secondary fw-medium" th:text="${{'$' + (entry.series{series}ClientInvoice != null ? #numbers.formatDecimal(entry.series{series}ClientInvoice, 1, 'COMMA', 2, 'POINT') : '0.00')}}"></span>
                                            </div>"""

    new_block = f"""                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">BID Amount</span>
                                                <span class="text-secondary fw-medium" th:text="${{'$' + (entry.series{series}BidAmount != null ? #numbers.formatDecimal(entry.series{series}BidAmount, 1, 'COMMA', 2, 'POINT') : '0.00')}}"></span>
                                            </div>
                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">BID count</span>
                                                <span class="text-secondary fw-medium" th:text="${{entry.series{series}BidCount}}"></span>
                                            </div>
                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">Client Inv</span>
                                                <span class="text-secondary fw-medium" th:text="${{'$' + (entry.series{series}ClientInvoice != null ? #numbers.formatDecimal(entry.series{series}ClientInvoice, 1, 'COMMA', 2, 'POINT') : '0.00')}}"></span>
                                            </div>
                                            <div class="d-flex justify-content-between mb-1" style="font-size: 0.75rem;">
                                                <span class="text-muted">Crew Invoice</span>
                                                <span class="text-secondary fw-medium" th:text="${{'$' + (entry.series{series}CrewInvoice != null ? #numbers.formatDecimal(entry.series{series}CrewInvoice, 1, 'COMMA', 2, 'POINT') : '0.00')}}"></span>
                                            </div>"""
    
    # We also want to update the min-width of the container to 135px so everything fits nicely.
    # The container line: <div class="bg-light rounded-3 p-2 border border-light-subtle shadow-sm mx-auto" style="min-width: 110px;">
    # Wait, maybe it's safer to just replace min-width: 110px with min-width: 135px for all series.
    content = content.replace(old_block, new_block)

content = content.replace('style="min-width: 110px;"', 'style="min-width: 140px;"')

with open('/home/mhcybroot/Projects/Skylink-custom-backend/src/main/resources/templates/admin-analyst-dashboard.html', 'w') as f:
    f.write(content)

print("Replacement complete.")
