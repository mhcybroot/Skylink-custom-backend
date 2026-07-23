package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalystPerformanceDto {
    private String analyst;
    private Integer totalAssignedWos = 0;
    private Integer totalWos = 0;
    private Integer bidCount = 0;
    private BigDecimal bidAmount = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private Integer preservationWoCount = 0;
    private Integer maintenanceWoCount = 0;
    
    // Dynamic categories
    private java.util.Map<String, Integer> dynamicCategoryCounts = new java.util.HashMap<>();

    private Integer othersWoCount = 0;

    public void incrementCategory(String category) {
        dynamicCategoryCounts.put(category, dynamicCategoryCounts.getOrDefault(category, 0) + 1);
    }

    public void incrementOthersWo() {
        this.othersWoCount++;
    }

    private Integer series100WoCount = 0;
    private Integer series200WoCount = 0;
    private Integer series300WoCount = 0;
    private Integer series400WoCount = 0;
    private Integer series500WoCount = 0;
    private Integer series600WoCount = 0;
    private Integer series700WoCount = 0;
    private Integer series800WoCount = 0;
    private Integer series900WoCount = 0;
    private Integer seriesOthersWoCount = 0;

    private BigDecimal series100BidAmount = BigDecimal.ZERO;
    private BigDecimal series200BidAmount = BigDecimal.ZERO;
    private BigDecimal series300BidAmount = BigDecimal.ZERO;
    private BigDecimal series400BidAmount = BigDecimal.ZERO;
    private BigDecimal series500BidAmount = BigDecimal.ZERO;
    private BigDecimal series600BidAmount = BigDecimal.ZERO;
    private BigDecimal series700BidAmount = BigDecimal.ZERO;
    private BigDecimal series800BidAmount = BigDecimal.ZERO;
    private BigDecimal series900BidAmount = BigDecimal.ZERO;
    private BigDecimal seriesOthersBidAmount = BigDecimal.ZERO;

    private BigDecimal series100ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series200ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series300ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series400ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series500ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series600ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series700ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series800ClientInvoice = BigDecimal.ZERO;
    private BigDecimal series900ClientInvoice = BigDecimal.ZERO;
    private BigDecimal seriesOthersClientInvoice = BigDecimal.ZERO;

    private BigDecimal series100CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series200CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series300CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series400CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series500CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series600CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series700CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series800CrewInvoice = BigDecimal.ZERO;
    private BigDecimal series900CrewInvoice = BigDecimal.ZERO;
    private BigDecimal seriesOthersCrewInvoice = BigDecimal.ZERO;

    private Integer series100BidCount = 0;
    private Integer series200BidCount = 0;
    private Integer series300BidCount = 0;
    private Integer series400BidCount = 0;
    private Integer series500BidCount = 0;
    private Integer series600BidCount = 0;
    private Integer series700BidCount = 0;
    private Integer series800BidCount = 0;
    private Integer series900BidCount = 0;
    private Integer seriesOthersBidCount = 0;

    private BigDecimal series100Profit = BigDecimal.ZERO;
    private BigDecimal series200Profit = BigDecimal.ZERO;
    private BigDecimal series300Profit = BigDecimal.ZERO;
    private BigDecimal series400Profit = BigDecimal.ZERO;
    private BigDecimal series500Profit = BigDecimal.ZERO;
    private BigDecimal series600Profit = BigDecimal.ZERO;
    private BigDecimal series700Profit = BigDecimal.ZERO;
    private BigDecimal series800Profit = BigDecimal.ZERO;
    private BigDecimal series900Profit = BigDecimal.ZERO;
    private BigDecimal seriesOthersProfit = BigDecimal.ZERO;

    public void incrementSeries(int seriesKey, Integer bidCount, BigDecimal bidAmount, BigDecimal clientInvoice, BigDecimal crewInvoice) {
        BigDecimal calculatedProfit = BigDecimal.ZERO;
        if (clientInvoice != null && crewInvoice != null) {
            calculatedProfit = clientInvoice.subtract(crewInvoice);
        } else if (clientInvoice != null) {
            calculatedProfit = clientInvoice;
        } else if (crewInvoice != null) {
            calculatedProfit = BigDecimal.ZERO.subtract(crewInvoice);
        }

        switch (seriesKey) {
            case 100: 
                this.series100WoCount++; 
                if (bidCount != null) this.series100BidCount += bidCount;
                if (bidAmount != null) this.series100BidAmount = this.series100BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series100ClientInvoice = this.series100ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series100CrewInvoice = this.series100CrewInvoice.add(crewInvoice);
                this.series100Profit = this.series100Profit.add(calculatedProfit);
                break;
            case 200: 
                this.series200WoCount++; 
                if (bidCount != null) this.series200BidCount += bidCount;
                if (bidAmount != null) this.series200BidAmount = this.series200BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series200ClientInvoice = this.series200ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series200CrewInvoice = this.series200CrewInvoice.add(crewInvoice);
                this.series200Profit = this.series200Profit.add(calculatedProfit);
                break;
            case 300: 
                this.series300WoCount++; 
                if (bidCount != null) this.series300BidCount += bidCount;
                if (bidAmount != null) this.series300BidAmount = this.series300BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series300ClientInvoice = this.series300ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series300CrewInvoice = this.series300CrewInvoice.add(crewInvoice);
                this.series300Profit = this.series300Profit.add(calculatedProfit);
                break;
            case 400: 
                this.series400WoCount++; 
                if (bidCount != null) this.series400BidCount += bidCount;
                if (bidAmount != null) this.series400BidAmount = this.series400BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series400ClientInvoice = this.series400ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series400CrewInvoice = this.series400CrewInvoice.add(crewInvoice);
                this.series400Profit = this.series400Profit.add(calculatedProfit);
                break;
            case 500: 
                this.series500WoCount++; 
                if (bidCount != null) this.series500BidCount += bidCount;
                if (bidAmount != null) this.series500BidAmount = this.series500BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series500ClientInvoice = this.series500ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series500CrewInvoice = this.series500CrewInvoice.add(crewInvoice);
                this.series500Profit = this.series500Profit.add(calculatedProfit);
                break;
            case 600: 
                this.series600WoCount++; 
                if (bidCount != null) this.series600BidCount += bidCount;
                if (bidAmount != null) this.series600BidAmount = this.series600BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series600ClientInvoice = this.series600ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series600CrewInvoice = this.series600CrewInvoice.add(crewInvoice);
                this.series600Profit = this.series600Profit.add(calculatedProfit);
                break;
            case 700: 
                this.series700WoCount++; 
                if (bidCount != null) this.series700BidCount += bidCount;
                if (bidAmount != null) this.series700BidAmount = this.series700BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series700ClientInvoice = this.series700ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series700CrewInvoice = this.series700CrewInvoice.add(crewInvoice);
                this.series700Profit = this.series700Profit.add(calculatedProfit);
                break;
            case 800: 
                this.series800WoCount++; 
                if (bidCount != null) this.series800BidCount += bidCount;
                if (bidAmount != null) this.series800BidAmount = this.series800BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series800ClientInvoice = this.series800ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series800CrewInvoice = this.series800CrewInvoice.add(crewInvoice);
                this.series800Profit = this.series800Profit.add(calculatedProfit);
                break;
            case 900: 
                this.series900WoCount++; 
                if (bidCount != null) this.series900BidCount += bidCount;
                if (bidAmount != null) this.series900BidAmount = this.series900BidAmount.add(bidAmount);
                if (clientInvoice != null) this.series900ClientInvoice = this.series900ClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.series900CrewInvoice = this.series900CrewInvoice.add(crewInvoice);
                this.series900Profit = this.series900Profit.add(calculatedProfit);
                break;
            default: 
                this.seriesOthersWoCount++; 
                if (bidCount != null) this.seriesOthersBidCount += bidCount;
                if (bidAmount != null) this.seriesOthersBidAmount = this.seriesOthersBidAmount.add(bidAmount);
                if (clientInvoice != null) this.seriesOthersClientInvoice = this.seriesOthersClientInvoice.add(clientInvoice);
                if (crewInvoice != null) this.seriesOthersCrewInvoice = this.seriesOthersCrewInvoice.add(crewInvoice);
                this.seriesOthersProfit = this.seriesOthersProfit.add(calculatedProfit);
                break;
        }
    }

    public void incrementTotalAssignedWos() {
        this.totalAssignedWos++;
    }

    public void incrementTotalWos() {
        this.totalWos++;
    }

    public void incrementPreservationWo() {
        this.preservationWoCount++;
    }

    public void incrementMaintenanceWo() {
        this.maintenanceWoCount++;
    }

    public void addBidCount(Integer count) {
        if (count != null) {
            this.bidCount += count;
        }
    }

    public void addBidAmount(BigDecimal amount) {
        if (amount != null) {
            this.bidAmount = this.bidAmount.add(amount);
        }
    }

    public void addGrossProfit(BigDecimal clientInv, BigDecimal crewInv) {
        if (clientInv != null && crewInv != null) {
            this.grossProfit = this.grossProfit.add(clientInv.subtract(crewInv));
        } else if (clientInv != null) {
            this.grossProfit = this.grossProfit.add(clientInv);
        } else if (crewInv != null) {
            this.grossProfit = this.grossProfit.subtract(crewInv);
        }
    }
}
