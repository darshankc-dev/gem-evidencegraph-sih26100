document.addEventListener('DOMContentLoaded', () => {

    const state = {
        activeBidId: null,
        activeBid: null,
        bidsList: [],
        bidsDataMap: new Map(),
        documents: [],
        claims: [],
        verificationResults: [],
        evidenceItems: [],
        complianceResult: null,
        temporalResult: null,
        riskAssessment: null,
        riskDimensions: [],
        entityResolutionResult: null
    };

    const elements = {

        healthDot: document.getElementById('systemHealthDot'),
        healthText: document.getElementById('systemHealthText'),
        healthSubtext: document.getElementById('systemHealthSubtext'),

        notificationBanner: document.getElementById('notificationBanner'),
        notificationMessage: document.getElementById('notificationMessage'),
        notificationIcon: document.getElementById('notificationIcon'),
        closeNotificationBtn: document.getElementById('closeNotificationBtn'),

        demoStatusBadge: document.getElementById('demoStatusBadge'),
        datasetTimestampInfo: document.getElementById('datasetTimestampInfo'),
        loadDemoDatasetBtn: document.getElementById('loadDemoDatasetBtn'),
        loadDemoBtnText: document.getElementById('loadDemoBtnText'),
        loadDemoSpinner: document.getElementById('loadDemoSpinner'),
        resetDemoDatasetBtn: document.getElementById('resetDemoDatasetBtn'),
        resetDemoBtnText: document.getElementById('resetDemoBtnText'),
        resetDemoSpinner: document.getElementById('resetDemoSpinner'),

        summaryTotalBids: document.getElementById('summaryTotalBids'),
        summaryCompliantBids: document.getElementById('summaryCompliantBids'),
        summaryReviewBids: document.getElementById('summaryReviewBids'),
        summaryHighRiskBids: document.getElementById('summaryHighRiskBids'),
        captionTotalBids: document.getElementById('captionTotalBids'),

        bidsTable: document.getElementById('bidsTable'),
        bidsTableBody: document.getElementById('bidsTableBody'),
        tableRecordCountBadge: document.getElementById('tableRecordCountBadge'),
        btnRefreshMatrix: document.getElementById('btnRefreshMatrix'),

        bidIdInput: document.getElementById('bidIdInput'),
        loadBidBtn: document.getElementById('loadBidBtn'),
        clearBidBtn: document.getElementById('clearBidBtn'),
        bidLookupStatus: document.getElementById('bidLookupStatus'),

        nodeDoc: document.getElementById('nodeDoc'),
        nodeStatusDoc: document.getElementById('nodeStatusDoc'),
        nodeClaim: document.getElementById('nodeClaim'),
        nodeStatusClaim: document.getElementById('nodeStatusClaim'),
        nodeEvidence: document.getElementById('nodeEvidence'),
        nodeStatusEvidence: document.getElementById('nodeStatusEvidence'),
        nodeVerif: document.getElementById('nodeVerif'),
        nodeStatusVerif: document.getElementById('nodeStatusVerif'),
        nodeEntity: document.getElementById('nodeEntity'),
        nodeStatusEntity: document.getElementById('nodeStatusEntity'),
        nodeComp: document.getElementById('nodeComp'),
        nodeStatusComp: document.getElementById('nodeStatusComp'),
        nodeTemporal: document.getElementById('nodeTemporal'),
        nodeStatusTemporal: document.getElementById('nodeStatusTemporal'),
        nodeRisk: document.getElementById('nodeRisk'),
        nodeStatusRisk: document.getElementById('nodeStatusRisk'),
        nodeDecision: document.getElementById('nodeDecision'),
        nodeStatusDecision: document.getElementById('nodeStatusDecision'),

        decisionBadge: document.getElementById('decisionBadge'),
        decisionScoreValue: document.getElementById('decisionScoreValue'),
        decisionLevelValue: document.getElementById('decisionLevelValue'),
        decisionLevelSub: document.getElementById('decisionLevelSub'),
        decisionHardFailValue: document.getElementById('decisionHardFailValue'),
        decisionHardFailSub: document.getElementById('decisionHardFailSub'),
        decisionComplianceRatio: document.getElementById('decisionComplianceRatio'),
        decisionComplianceStatus: document.getElementById('decisionComplianceStatus'),
        decisionConciseFindings: document.getElementById('decisionConciseFindings'),
        decisionFullReasonToggle: document.getElementById('decisionFullReasonToggle'),
        fullReasonToggleLabel: document.getElementById('fullReasonToggleLabel'),
        decisionReasonText: document.getElementById('decisionReasonText'),
        decisionSummaryText: document.getElementById('decisionSummaryText'),

        btnRunVerification: document.getElementById('btnRunVerification'),
        spinnerVerification: document.getElementById('spinnerVerification'),
        btnEvaluateCompliance: document.getElementById('btnEvaluateCompliance'),
        spinnerCompliance: document.getElementById('spinnerCompliance'),
        btnRunTemporal: document.getElementById('btnRunTemporal'),
        spinnerTemporal: document.getElementById('spinnerTemporal'),
        btnEvaluateRisk: document.getElementById('btnEvaluateRisk'),
        spinnerRisk: document.getElementById('spinnerRisk'),
        btnRefreshAssessment: document.getElementById('btnRefreshAssessment'),
        spinnerRefresh: document.getElementById('spinnerRefresh'),

        activeBidRefBadge: document.getElementById('activeBidRefBadge'),
        activeBidderNameBadge: document.getElementById('activeBidderNameBadge'),

        overviewBidderName: document.getElementById('overviewBidderName'),
        overviewBidRef: document.getElementById('overviewBidRef'),
        overviewTenderRef: document.getElementById('overviewTenderRef'),
        overviewTenderTitle: document.getElementById('overviewTenderTitle'),
        overviewSubmissionDate: document.getElementById('overviewSubmissionDate'),
        overviewBidStatus: document.getElementById('overviewBidStatus'),

        complianceOverallBadge: document.getElementById('complianceOverallBadge'),
        complianceRatioValue: document.getElementById('complianceRatioValue'),
        complianceMandatoryCount: document.getElementById('complianceMandatoryCount'),
        complianceCompliantCount: document.getElementById('complianceCompliantCount'),
        complianceNonCompliantCount: document.getElementById('complianceNonCompliantCount'),
        complianceTableBody: document.getElementById('complianceTableBody'),

        dimScoreEligibility: document.getElementById('dimScoreEligibility'),
        dimBadgeEligibility: document.getElementById('dimBadgeEligibility'),
        dimFindingsEligibility: document.getElementById('dimFindingsEligibility'),
        dimSummaryEligibility: document.getElementById('dimSummaryEligibility'),

        dimScoreDocIntegrity: document.getElementById('dimScoreDocIntegrity'),
        dimBadgeDocIntegrity: document.getElementById('dimBadgeDocIntegrity'),
        dimFindingsDocIntegrity: document.getElementById('dimFindingsDocIntegrity'),
        dimSummaryDocIntegrity: document.getElementById('dimSummaryDocIntegrity'),

        dimScoreConsistency: document.getElementById('dimScoreConsistency'),
        dimBadgeConsistency: document.getElementById('dimBadgeConsistency'),
        dimFindingsConsistency: document.getElementById('dimFindingsConsistency'),
        dimSummaryConsistency: document.getElementById('dimSummaryConsistency'),

        dimScoreTemporal: document.getElementById('dimScoreTemporal'),
        dimBadgeTemporal: document.getElementById('dimBadgeTemporal'),
        dimFindingsTemporal: document.getElementById('dimFindingsTemporal'),
        dimSummaryTemporal: document.getElementById('dimSummaryTemporal'),

        dimScoreVerification: document.getElementById('dimScoreVerification'),
        dimBadgeVerification: document.getElementById('dimBadgeVerification'),
        dimFindingsVerification: document.getElementById('dimFindingsVerification'),
        dimSummaryVerification: document.getElementById('dimSummaryVerification'),

        entityMatchStatusBadge: document.getElementById('entityMatchStatusBadge'),
        entityConfidenceValue: document.getElementById('entityConfidenceValue'),
        entityExplanationText: document.getElementById('entityExplanationText'),
        riskFindingsTableBody: document.getElementById('riskFindingsTableBody'),

        tempTotalCount: document.getElementById('tempTotalCount'),
        tempValidCount: document.getElementById('tempValidCount'),
        tempExpiredCount: document.getElementById('tempExpiredCount'),
        tempFutureCount: document.getElementById('tempFutureCount'),
        tempPostTenderCount: document.getElementById('tempPostTenderCount'),
        tempPostBidCount: document.getElementById('tempPostBidCount'),
        tempConflictCount: document.getElementById('tempConflictCount'),
        tempNotAppCount: document.getElementById('tempNotAppCount'),
        temporalTableBody: document.getElementById('temporalTableBody'),

        claimsTableBody: document.getElementById('claimsTableBody'),

        evidenceTableBody: document.getElementById('evidenceTableBody'),
        evidenceGraphContainer: document.getElementById('evidenceGraphContainer'),
        evidenceEmptyState: document.getElementById('evidenceEmptyState'),
        evidenceGrid: document.getElementById('evidenceGrid'),

        documentsTableBody: document.getElementById('documentsTableBody'),

        docDetailsModal: document.getElementById('docDetailsModal'),
        modalDocTitle: document.getElementById('modalDocTitle'),
        modalDocBody: document.getElementById('modalDocBody'),
        closeDocModalBtn: document.getElementById('closeDocModalBtn'),
        closeDocModalFooterBtn: document.getElementById('closeDocModalFooterBtn')
    };

    async function checkHealth() {
        try {
            const res = await fetch('/api/health');
            if (res.ok) {
                const data = await res.json();
                if (data.status === 'UP') {
                    elements.healthDot.className = 'status-dot dot-operational';
                    elements.healthText.textContent = 'System Operational';
                    elements.healthSubtext.textContent = 'PostgreSQL / Core Engine UP';
                    return;
                }
            }
            elements.healthDot.className = 'status-dot dot-degraded';
            elements.healthText.textContent = 'System Degraded';
            elements.healthSubtext.textContent = 'Database or component alert';
        } catch (err) {
            elements.healthDot.className = 'status-dot dot-degraded';
            elements.healthText.textContent = 'System Offline';
            elements.healthSubtext.textContent = 'Unable to reach /api/health';
        }
    }

    async function checkDemoStatus() {
        try {
            const res = await fetch('/api/demo/status');
            if (res.ok) {
                const data = await res.json();
                if (data.isLoaded) {
                    elements.demoStatusBadge.textContent = 'Status: Loaded (3 Synthetic Bids)';
                    elements.demoStatusBadge.className = 'badge badge-positive';
                    elements.datasetTimestampInfo.textContent = `Backend State: Ingested (${data.count || 3} bids in database)`;
                } else {
                    elements.demoStatusBadge.textContent = 'Status: Not Loaded';
                    elements.demoStatusBadge.className = 'badge badge-outline';
                    elements.datasetTimestampInfo.textContent = 'Backend State: Clean / Demo data not loaded';
                }
            }
        } catch (err) {
            elements.demoStatusBadge.textContent = 'Status: Offline';
            elements.demoStatusBadge.className = 'badge badge-negative';
            elements.datasetTimestampInfo.textContent = 'Backend State: Connection failed';
        }
    }

    async function handleLoadDemoDataset() {
        try {
            elements.loadDemoDatasetBtn.disabled = true;
            elements.loadDemoBtnText.textContent = 'Ingesting Pipeline...';
            setSpinner(elements.loadDemoSpinner, true);

            const res = await fetch('/api/demo/load', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });

            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `Server returned HTTP ${res.status}`);
            }

            const data = await res.json();
            showNotification(data.message || 'Synthetic demonstration dataset successfully loaded.', 'success', 8000);

            await refreshAllDashboardData();

            if (state.bidsList.length > 0) {
                const apexBid = state.bidsList.find(b => b.bidReference === 'GEM-DEMO-BID-001') || state.bidsList[0];
                if (apexBid && apexBid.bidId) {
                    await selectBidForAnalysis(apexBid.bidId);
                }
            }
        } catch (err) {
            console.error('Error loading demo dataset:', err);
            showNotification(`Unable to retrieve bid analysis. Please verify that the backend is running. (${err.message})`, 'error', 9000);
        } finally {
            elements.loadDemoDatasetBtn.disabled = false;
            elements.loadDemoBtnText.textContent = 'Load Demo Dataset';
            setSpinner(elements.loadDemoSpinner, false);
        }
    }

    async function handleResetDemoDataset() {
        if (!confirm('Are you sure you want to reset the synthetic demonstration dataset? This will remove records prefixed with GEM-DEMO-.')) {
            return;
        }

        try {
            elements.resetDemoDatasetBtn.disabled = true;
            elements.resetDemoBtnText.textContent = 'Resetting...';
            setSpinner(elements.resetDemoSpinner, true);

            const res = await fetch('/api/demo/reset', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });

            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `Server returned HTTP ${res.status}`);
            }

            const data = await res.json();
            showNotification(data.message || 'Synthetic demo dataset successfully reset.', 'info', 6000);

            clearActiveBid();
            await refreshAllDashboardData();
        } catch (err) {
            console.error('Error resetting demo dataset:', err);
            showNotification(`Unable to reset demo dataset: ${err.message}`, 'error', 8000);
        } finally {
            elements.resetDemoDatasetBtn.disabled = false;
            elements.resetDemoBtnText.textContent = 'Reset Demo Dataset';
            setSpinner(elements.resetDemoSpinner, false);
        }
    }

    async function refreshAllDashboardData() {
        await checkDemoStatus();
        await loadBidsMatrixAndSummary();
    }

    async function loadBidsMatrixAndSummary() {
        try {
            const res = await fetch('/api/bids');
            if (!res.ok) {
                throw new Error(`Failed to fetch /api/bids with status ${res.status}`);
            }

            const bids = await res.json();
            state.bidsList = Array.isArray(bids) ? bids : [];
            state.bidsDataMap.clear();

            if (state.bidsList.length === 0) {
                renderEmptyBidsMatrix();
                updateSummaryKPIs(0, 0, 0, 0);
                return;
            }

            const fetchPromises = state.bidsList.map(async (bid) => {
                let compliance = null;
                let risk = null;
                try {
                    const cRes = await fetch(`/api/bids/${bid.bidId}/compliance`);
                    if (cRes.ok) compliance = await cRes.json();
                } catch {
                    compliance = null;
                }

                try {
                    const rRes = await fetch(`/api/bids/${bid.bidId}/risk`);
                    if (rRes.ok) risk = await rRes.json();
                } catch {
                    risk = null;
                }

                state.bidsDataMap.set(bid.bidId, { bid, compliance, risk });
                return { bid, compliance, risk };
            });

            const rowsData = await Promise.all(fetchPromises);

            let compliantCount = 0;
            let reviewCount = 0;
            let highRiskCount = 0;

            rowsData.forEach(({ compliance, risk }) => {
                const rec = risk ? risk.decisionRecommendation : null;
                const compStatus = compliance ? compliance.overallStatus : null;

                if (rec === 'RECOMMEND' || compStatus === 'COMPLIANT') {
                    compliantCount++;
                } else if (rec === 'HIGH_RISK' || compStatus === 'NON_COMPLIANT') {
                    highRiskCount++;
                } else if (rec === 'REVIEW' || compStatus === 'PARTIALLY_COMPLIANT') {
                    reviewCount++;
                }
            });

            updateSummaryKPIs(state.bidsList.length, compliantCount, reviewCount, highRiskCount);
            renderBidsMatrixTable(rowsData);

        } catch (err) {
            console.error('Error loading bids matrix:', err);
            showNotification('Unable to retrieve bid analysis. Please verify that the backend is running.', 'error', 8000);
            renderEmptyBidsMatrix();
            updateSummaryKPIs(0, 0, 0, 0);
        }
    }

    function updateSummaryKPIs(total, compliant, review, highRisk) {
        elements.summaryTotalBids.textContent = total.toString();
        elements.summaryCompliantBids.textContent = compliant.toString();
        elements.summaryReviewBids.textContent = review.toString();
        elements.summaryHighRiskBids.textContent = highRisk.toString();
        elements.captionTotalBids.textContent = total > 0 ? `Calculated from ${total} active records` : 'Calculated from /api/bids';
        elements.tableRecordCountBadge.textContent = `${total} records`;
    }

    function renderEmptyBidsMatrix() {
        elements.bidsTableBody.innerHTML = `
            <tr>
                <td colspan="7" class="table-empty-cell">
                    <div class="empty-state-card" id="bidsEmptyState">
                        <div class="empty-state-icon">📋</div>
                        <h3 class="empty-state-title">No Bid Records Available</h3>
                        <p class="empty-state-desc">Click <strong>"Load Demo Dataset"</strong> above to ingest the SIH26100 synthetic demonstration dataset.</p>
                    </div>
                </td>
            </tr>
        `;
    }

    function renderBidsMatrixTable(rowsData) {
        let rowsHtml = '';
        rowsData.forEach(({ bid, compliance, risk }) => {
            const bidder = escapeHtml(bid.bidderLegalName || '—');
            const bidRef = escapeHtml(bid.bidReference || '—');
            const compBadge = compliance ? renderStatusBadge(compliance.overallStatus) : '<span class="badge badge-gray">PENDING</span>';
            const riskBadge = risk ? renderStatusBadge(risk.overallRiskLevel) : '<span class="badge badge-gray">—</span>';
            const scoreVal = (risk && risk.overallRiskScore != null) ? `<strong>${risk.overallRiskScore}</strong> <span style="font-size:0.75rem; color:var(--text-muted)">/ 100</span>` : '—';
            const recBadge = risk ? renderStatusBadge(risk.decisionRecommendation) : '<span class="badge badge-gray">—</span>';
            const isSelected = state.activeBidId === bid.bidId;

            rowsHtml += `
                <tr class="${isSelected ? 'selected-row' : ''}" id="bidRow-${bid.bidId}">
                    <td><strong>${bidder}</strong></td>
                    <td class="font-mono">${bidRef}</td>
                    <td>${compBadge}</td>
                    <td>${riskBadge}</td>
                    <td>${scoreVal}</td>
                    <td>${recBadge}</td>
                    <td style="text-align: center;">
                        <button type="button" class="btn btn-sm btn-primary view-analysis-btn" data-bid-id="${bid.bidId}">
                            View Analysis
                        </button>
                    </td>
                </tr>
            `;
        });

        elements.bidsTableBody.innerHTML = rowsHtml;

        elements.bidsTableBody.querySelectorAll('.view-analysis-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const bidId = btn.getAttribute('data-bid-id');
                selectBidForAnalysis(bidId);
            });
        });
    }

    async function selectBidForAnalysis(bidId) {
        if (!bidId) return;

        const cleanId = bidId.trim();
        elements.bidLookupStatus.textContent = `Querying backend for bid ${cleanId}...`;

        document.querySelectorAll('#bidsTableBody tr').forEach(tr => tr.classList.remove('selected-row'));
        const activeRow = document.getElementById(`bidRow-${cleanId}`);
        if (activeRow) activeRow.classList.add('selected-row');

        try {
            const res = await fetch(`/api/bids/${cleanId}`);
            if (res.status === 404) {
                showNotification(`Bid with ID '${cleanId}' was not found in the database.`, 'error');
                elements.bidLookupStatus.textContent = 'Bid not found. Please verify the UUID or load demo records.';
                return;
            }
            if (!res.ok) {
                throw new Error(`Server returned HTTP ${res.status}`);
            }

            const bidData = await res.json();
            state.activeBidId = bidData.bidId;
            state.activeBid = bidData;
            elements.bidIdInput.value = bidData.bidId;
            elements.bidLookupStatus.textContent = `Active Bid: ${bidData.bidReference} (${bidData.bidderLegalName || ''})`;

            elements.activeBidRefBadge.textContent = bidData.bidReference || 'ACTIVE BID';
            elements.activeBidderNameBadge.textContent = bidData.bidderLegalName || 'Bidder Legal Entity';

            elements.overviewBidderName.textContent = bidData.bidderLegalName || '—';
            elements.overviewBidRef.textContent = bidData.bidReference || '—';
            elements.overviewTenderRef.textContent = bidData.tenderReference || '—';
            elements.overviewTenderTitle.textContent = bidData.tenderTitle || '—';
            elements.overviewSubmissionDate.textContent = formatDate(bidData.submissionDate);
            elements.overviewBidStatus.innerHTML = renderStatusBadge(bidData.status);

            setActionButtonsState(true);

            await refreshActiveBidDetailedLayers(cleanId);

        } catch (err) {
            console.error('Error selecting bid:', err);
            showNotification(`Unable to retrieve bid analysis. Please verify that the backend is running. (${err.message})`, 'error');
            elements.bidLookupStatus.textContent = 'Error querying backend for bid.';
        }
    }

    async function refreshActiveBidDetailedLayers(bidId) {
        setSpinner(elements.spinnerRefresh, true);
        try {
            await Promise.allSettled([
                loadCompliance(bidId),
                loadRisk(bidId),
                loadTemporal(bidId),
                loadDocumentsAndClaims(bidId),
                loadEvidence(bidId)
            ]);

            if (state.activeBid && state.activeBid.bidderId) {
                await loadEntityResolution(state.activeBid.bidderId);
            }
        } finally {
            setSpinner(elements.spinnerRefresh, false);
        }
    }

    function clearActiveBid() {
        state.activeBidId = null;
        state.activeBid = null;
        state.documents = [];
        state.claims = [];
        state.verificationResults = [];
        state.evidenceItems = [];
        state.complianceResult = null;
        state.temporalResult = null;
        state.riskAssessment = null;
        state.riskDimensions = [];
        state.entityResolutionResult = null;

        elements.bidIdInput.value = '';
        elements.bidLookupStatus.textContent = 'Select a bid above or query by unique UUID.';
        elements.activeBidRefBadge.textContent = 'NO BID SELECTED';
        elements.activeBidderNameBadge.textContent = 'Select a bid above';

        elements.overviewBidderName.textContent = '—';
        elements.overviewBidRef.textContent = '—';
        elements.overviewTenderRef.textContent = '—';
        elements.overviewTenderTitle.textContent = '—';
        elements.overviewSubmissionDate.textContent = '—';
        elements.overviewBidStatus.innerHTML = '<span class="badge badge-gray">—</span>';

        resetDecisionHeroCard();

        resetArchitectureFlow();

        renderEmptyTabDossier();

        setActionButtonsState(false);

        document.querySelectorAll('#bidsTableBody tr').forEach(tr => tr.classList.remove('selected-row'));
    }

    function setActionButtonsState(enabled) {
        elements.btnRunVerification.disabled = !enabled;
        elements.btnEvaluateCompliance.disabled = !enabled;
        elements.btnRunTemporal.disabled = !enabled;
        elements.btnEvaluateRisk.disabled = !enabled;
        elements.btnRefreshAssessment.disabled = !enabled;
    }

    async function loadCompliance(bidId) {
        try {
            const res = await fetch(`/api/bids/${bidId}/compliance`);
            if (res.ok) {
                const data = await res.json();
                state.complianceResult = data;
                renderCompliance(data);

                const statusClass = data.overallStatus === 'COMPLIANT' ? 'completed-node' : (data.overallStatus === 'NON_COMPLIANT' ? 'alert-node' : 'active-node');
                updateArchNode('nodeComp', 'nodeStatusComp', data.overallStatus || 'Evaluated', statusClass);
            } else {
                state.complianceResult = null;
                renderCompliance(null);
                updateArchNode('nodeComp', 'nodeStatusComp', 'Pending', '');
            }
        } catch (err) {
            state.complianceResult = null;
            renderCompliance(null);
            updateArchNode('nodeComp', 'nodeStatusComp', 'Offline', 'alert-node');
        }
    }

    function renderCompliance(data) {
        if (!data) {
            elements.complianceOverallBadge.innerHTML = '<span class="badge badge-gray">—</span>';
            elements.complianceRatioValue.textContent = '—';
            elements.complianceMandatoryCount.textContent = '—';
            elements.complianceCompliantCount.textContent = '—';
            elements.complianceNonCompliantCount.textContent = '—';
            elements.decisionComplianceRatio.textContent = '—';
            elements.decisionComplianceStatus.textContent = 'Awaiting evaluation';
            elements.complianceTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="table-empty-cell">
                        <div class="empty-state-inline">Compliance assessment has not been performed yet.</div>
                    </td>
                </tr>
            `;
            return;
        }

        elements.complianceOverallBadge.innerHTML = renderStatusBadge(data.overallStatus);
        const ratioStr = data.compliancePercentage != null ? `${data.compliancePercentage.toFixed(1)}%` : '—';
        elements.complianceRatioValue.textContent = ratioStr;
        elements.complianceMandatoryCount.textContent = data.mandatoryRequirementCount != null ? data.mandatoryRequirementCount.toString() : '0';
        elements.complianceCompliantCount.textContent = data.compliantCount != null ? data.compliantCount.toString() : '0';
        elements.complianceNonCompliantCount.textContent = data.nonCompliantCount != null ? data.nonCompliantCount.toString() : '0';

        elements.decisionComplianceRatio.textContent = ratioStr;
        elements.decisionComplianceStatus.textContent = data.overallStatus || 'EVALUATED';

        if (data.requirementResults && data.requirementResults.length > 0) {
            let rowsHtml = '';
            data.requirementResults.forEach(r => {
                rowsHtml += `
                    <tr>
                        <td class="font-mono"><strong>${escapeHtml(r.requirementCode || '—')}</strong></td>
                        <td>${escapeHtml(r.requirementName || '—')}</td>
                        <td><span class="badge badge-gray">${escapeHtml(r.requirementType || '—')}</span></td>
                        <td>${r.mandatory ? '<span class="badge badge-amber">YES</span>' : '<span class="badge badge-gray">NO</span>'}</td>
                        <td>${renderStatusBadge(r.status)}</td>
                        <td>${escapeHtml(r.reason || '—')}</td>
                    </tr>
                `;
            });
            elements.complianceTableBody.innerHTML = rowsHtml;
        } else {
            elements.complianceTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="table-empty-cell">
                        <div class="empty-state-inline">No individual requirement evaluations found.</div>
                    </td>
                </tr>
            `;
        }
    }

    async function loadRisk(bidId) {
        try {
            const [riskRes, dimsRes] = await Promise.all([
                fetch(`/api/bids/${bidId}/risk`),
                fetch(`/api/bids/${bidId}/risk/dimensions`)
            ]);

            if (riskRes.ok) {
                const riskData = await riskRes.json();
                state.riskAssessment = riskData;
                renderDecisionHeroCard(riskData);

                const riskClass = riskData.overallRiskLevel === 'LOW' ? 'completed-node' : (riskData.overallRiskLevel === 'HIGH' ? 'alert-node' : 'active-node');
                updateArchNode('nodeRisk', 'nodeStatusRisk', `Score: ${riskData.overallRiskScore}`, riskClass);

                const decClass = riskData.decisionRecommendation === 'RECOMMEND' ? 'completed-node' : (riskData.decisionRecommendation === 'HIGH_RISK' ? 'alert-node' : 'active-node');
                updateArchNode('nodeDecision', 'nodeStatusDecision', riskData.decisionRecommendation || 'DECISION', decClass);
            } else {
                state.riskAssessment = null;
                renderDecisionHeroCard(null);
                updateArchNode('nodeRisk', 'nodeStatusRisk', 'Pending', '');
                updateArchNode('nodeDecision', 'nodeStatusDecision', 'Pending', '');
            }

            if (dimsRes.ok) {
                const dimsData = await dimsRes.json();
                state.riskDimensions = dimsData;
                renderRiskDimensions(dimsData);
            } else {
                state.riskDimensions = [];
                renderRiskDimensions([]);
            }
        } catch (err) {
            state.riskAssessment = null;
            state.riskDimensions = [];
            renderDecisionHeroCard(null);
            renderRiskDimensions([]);
            updateArchNode('nodeRisk', 'nodeStatusRisk', 'Offline', 'alert-node');
            updateArchNode('nodeDecision', 'nodeStatusDecision', 'Offline', 'alert-node');
        }
    }

    function renderDecisionHeroCard(data) {
        if (!data) {
            resetDecisionHeroCard();
            return;
        }

        const score = data.overallRiskScore != null ? data.overallRiskScore : '—';
        elements.decisionScoreValue.textContent = score;

        const level = data.overallRiskLevel || '—';
        elements.decisionLevelValue.textContent = level;
        elements.decisionLevelSub.textContent = `Deterministic Level: ${level}`;

        const isHardFail = Boolean(data.hardFail);
        elements.decisionHardFailValue.textContent = isHardFail ? 'YES (TRIGGERED)' : 'None';
        elements.decisionHardFailValue.className = isHardFail ? 'decision-metric-val text-red' : 'decision-metric-val text-green';
        elements.decisionHardFailSub.textContent = isHardFail ? 'Statutory Disqualification' : 'No disqualifying findings';

        const rec = data.decisionRecommendation || 'REVIEW';
        elements.decisionBadge.textContent = rec;
        if (rec === 'RECOMMEND') {
            elements.decisionBadge.className = 'decision-badge badge-recommend';
        } else if (rec === 'HIGH_RISK') {
            elements.decisionBadge.className = 'decision-badge badge-highrisk';
        } else {
            elements.decisionBadge.className = 'decision-badge badge-review';
        }

        renderConciseFindings(data);

        elements.decisionReasonText.textContent = data.reason || 'All evaluated dimensions satisfy deterministic thresholds.';
        elements.decisionSummaryText.textContent = data.summary || `Risk evaluation: Score ${score}/100, Level ${level}, Decision: ${rec}.`;

        if (elements.decisionFullReasonToggle) {
            elements.decisionFullReasonToggle.open = false;
        }
    }

    function resetDecisionHeroCard() {
        elements.decisionBadge.className = 'decision-badge badge-pending';
        elements.decisionBadge.textContent = 'AWAITING SELECTION';
        elements.decisionScoreValue.textContent = '—';
        elements.decisionLevelValue.textContent = '—';
        elements.decisionLevelSub.textContent = 'Threshold-based';
        elements.decisionHardFailValue.textContent = '—';
        elements.decisionHardFailValue.className = 'decision-metric-val';
        elements.decisionHardFailSub.textContent = 'Statutory disqualifier';
        elements.decisionComplianceRatio.textContent = '—';
        elements.decisionComplianceStatus.textContent = '—';

        if (elements.decisionConciseFindings) {
            elements.decisionConciseFindings.innerHTML = `
                <p class="reason-text-placeholder">
                    Select a bid from the matrix above and click "View Analysis" to inspect the synthesized decision assessment.
                </p>
            `;
        }

        elements.decisionReasonText.textContent = 'Select a bid from the matrix above and click "View Analysis" to inspect the synthesized decision assessment.';
        elements.decisionSummaryText.textContent = 'Awaiting active bid selection.';

        if (elements.decisionFullReasonToggle) {
            elements.decisionFullReasonToggle.open = false;
        }
    }

    function renderConciseFindings(data) {
        if (!elements.decisionConciseFindings) return;

        if (!data) {
            elements.decisionConciseFindings.innerHTML = `
                <p class="reason-text-placeholder">
                    Select a bid from the matrix above and click "View Analysis" to inspect the synthesized decision assessment.
                </p>
            `;
            return;
        }

        const bullets = [];

        const recBadge = renderStatusBadge(data.decisionRecommendation);
        const riskBadge = renderStatusBadge(data.overallRiskLevel);

        bullets.push(`<strong>Overall Risk:</strong> ${riskBadge} <span class="font-mono" style="margin-left:0.25rem;">(Score: ${data.overallRiskScore != null ? data.overallRiskScore : 0} / 100)</span>`);
        bullets.push(`<strong>Decision Recommendation:</strong> ${recBadge}`);

        if (data.hardFail) {
            bullets.push(`<strong>Statutory Hard-Fail:</strong> <span class="badge badge-negative font-bold">TRIGGERED</span> — Disqualifying condition detected`);
        } else {
            bullets.push(`<strong>Statutory Hard-Fail:</strong> <span class="badge badge-positive">None</span> — No statutory disqualifiers`);
        }

        if (data.reason) {
            const raw = data.reason.trim();

            if (raw.toLowerCase().startsWith('applicable deterministic checks have been satisfied')) {
                bullets.push(`<span>All evaluated statutory dimensions satisfy deterministic thresholds without alerts.</span>`);
            } else {

                const cleaned = raw
                    .replace(/^Review required:\s*Unresolved items require procurement officer attention:\s*/i, '')
                    .replace(/^Hard-fail condition detected:\s*/i, '')
                    .trim();

                const parts = cleaned.split(';').map(p => p.trim()).filter(p => p.length > 0);
                parts.forEach(part => {

                    if (!part.toLowerCase().startsWith('overall cumulative risk')) {
                        bullets.push(`<span>${escapeHtml(part)}</span>`);
                    }
                });
            }
        }

        elements.decisionConciseFindings.innerHTML = `
            <ul class="concise-findings-list">
                ${bullets.map(b => `<li><span class="finding-bullet">•</span><div class="finding-content">${b}</div></li>`).join('')}
            </ul>
        `;
    }

    function renderRiskDimensions(dims) {
        const dimMap = {
            ELIGIBILITY: { score: elements.dimScoreEligibility, badge: elements.dimBadgeEligibility, findings: elements.dimFindingsEligibility, summary: elements.dimSummaryEligibility },
            DOCUMENT_INTEGRITY: { score: elements.dimScoreDocIntegrity, badge: elements.dimBadgeDocIntegrity, findings: elements.dimFindingsDocIntegrity, summary: elements.dimSummaryDocIntegrity },
            CONSISTENCY: { score: elements.dimScoreConsistency, badge: elements.dimBadgeConsistency, findings: elements.dimFindingsConsistency, summary: elements.dimSummaryConsistency },
            TEMPORAL: { score: elements.dimScoreTemporal, badge: elements.dimBadgeTemporal, findings: elements.dimFindingsTemporal, summary: elements.dimSummaryTemporal },
            VERIFICATION: { score: elements.dimScoreVerification, badge: elements.dimBadgeVerification, findings: elements.dimFindingsVerification, summary: elements.dimSummaryVerification }
        };

        Object.keys(dimMap).forEach(key => {
            const dom = dimMap[key];
            if (dom.score) dom.score.textContent = '—';
            if (dom.badge) dom.badge.outerHTML = `<span class="badge badge-gray" id="${dom.badge.id}">—</span>`;
            if (dom.findings) dom.findings.textContent = '0 findings';
            if (dom.summary) dom.summary.textContent = 'Awaiting evaluation';
        });

        const allFindings = [];

        if (Array.isArray(dims) && dims.length > 0) {
            dims.forEach(dim => {
                const dom = dimMap[dim.dimension];
                if (dom) {
                    if (dom.score) dom.score.textContent = dim.riskScore != null ? dim.riskScore : '0';
                    const badgeEl = document.getElementById(dom.badge.id || `dimBadge${dim.dimension}`);
                    if (badgeEl) {
                        badgeEl.outerHTML = renderStatusBadge(dim.riskLevel, badgeEl.id);
                    }
                    if (dom.findings) {
                        const count = dim.findingCount != null ? dim.findingCount : (dim.findings ? dim.findings.length : 0);
                        dom.findings.textContent = `${count} finding${count === 1 ? '' : 's'}`;
                    }
                    if (dom.summary) dom.summary.textContent = dim.summary || 'Dimension evaluated';
                }

                if (dim.findings && Array.isArray(dim.findings)) {
                    allFindings.push(...dim.findings);
                }
            });
        }

        renderRiskFindingsTable(allFindings);
    }

    function renderRiskFindingsTable(findings) {
        if (!findings || findings.length === 0) {
            elements.riskFindingsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">No auditable risk findings recorded for this bid.</div>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        findings.forEach(f => {
            rowsHtml += `
                <tr>
                    <td class="font-mono"><strong>${escapeHtml(f.code || '—')}</strong></td>
                    <td><span class="badge badge-gray">${escapeHtml(f.dimension || '—')}</span></td>
                    <td>${renderStatusBadge(f.severity)}</td>
                    <td><strong>${escapeHtml(f.title || '—')}</strong></td>
                    <td>${escapeHtml(f.description || '—')}</td>
                    <td><span class="badge badge-gray font-mono">${escapeHtml(f.rule || '—')}</span></td>
                    <td><strong>${f.weight != null ? f.weight : '—'}</strong></td>
                </tr>
            `;
        });
        elements.riskFindingsTableBody.innerHTML = rowsHtml;
    }

    async function loadTemporal(bidId) {
        try {
            const res = await fetch(`/api/bids/${bidId}/temporal`);
            if (res.ok) {
                const data = await res.json();
                state.temporalResult = data;
                renderTemporal(data);

                const hasAlerts = (data.expiredCount > 0 || data.conflictingCount > 0 || data.issuedAfterBidCount > 0);
                const statusClass = hasAlerts ? 'alert-node' : (data.validCount > 0 ? 'completed-node' : 'active-node');
                const summaryTxt = `${data.validCount || 0} Valid, ${data.expiredCount || 0} Expired`;
                updateArchNode('nodeTemporal', 'nodeStatusTemporal', summaryTxt, statusClass);
            } else {
                state.temporalResult = null;
                renderTemporal(null);
                updateArchNode('nodeTemporal', 'nodeStatusTemporal', 'Pending', '');
            }
        } catch (err) {
            state.temporalResult = null;
            renderTemporal(null);
            updateArchNode('nodeTemporal', 'nodeStatusTemporal', 'Offline', 'alert-node');
        }
    }

    function renderTemporal(data) {
        if (!data) {
            elements.tempTotalCount.textContent = '—';
            elements.tempValidCount.textContent = '—';
            elements.tempExpiredCount.textContent = '—';
            elements.tempFutureCount.textContent = '—';
            if (elements.tempPostTenderCount) elements.tempPostTenderCount.textContent = '—';
            elements.tempPostBidCount.textContent = '—';
            elements.tempConflictCount.textContent = '—';
            elements.tempNotAppCount.textContent = '—';

            elements.temporalTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">Temporal verification has not been performed yet.</div>
                    </td>
                </tr>
            `;
            return;
        }

        elements.tempTotalCount.textContent = (data.totalEvaluated != null ? data.totalEvaluated : 0).toString();
        elements.tempValidCount.textContent = (data.validCount != null ? data.validCount : 0).toString();
        elements.tempExpiredCount.textContent = (data.expiredCount != null ? data.expiredCount : 0).toString();
        elements.tempFutureCount.textContent = (data.futureValidityCount != null ? data.futureValidityCount : 0).toString();
        if (elements.tempPostTenderCount) {
            elements.tempPostTenderCount.textContent = (data.issuedAfterTenderPublicationCount != null ? data.issuedAfterTenderPublicationCount : 0).toString();
        }
        elements.tempPostBidCount.textContent = (data.issuedAfterBidCount != null ? data.issuedAfterBidCount : 0).toString();
        elements.tempConflictCount.textContent = (data.conflictingCount != null ? data.conflictingCount : 0).toString();
        elements.tempNotAppCount.textContent = (data.notApplicableCount != null ? data.notApplicableCount : 0).toString();

        if (data.results && data.results.length > 0) {
            let rowsHtml = '';
            data.results.forEach(r => {

                const formattedIssueDate = formatDateOnly(r.issueDate);
                const formattedValidFrom = formatDateOnly(r.validFrom);
                const formattedValidUntil = formatDateOnly(r.validUntil);
                const formattedRefDate = formatDateOnly(r.referenceDate);

                rowsHtml += `
                    <tr>
                        <td class="font-mono">${escapeHtml(r.evidenceId ? r.evidenceId.substring(0, 8) + '...' : '—')}</td>
                        <td><strong>${formattedIssueDate}</strong></td>
                        <td>${formattedValidFrom}</td>
                        <td>${formattedValidUntil}</td>
                        <td>${formattedRefDate}</td>
                        <td>${renderStatusBadge(r.status)}</td>
                        <td>${escapeHtml(r.reason || '—')}</td>
                    </tr>
                `;
            });
            elements.temporalTableBody.innerHTML = rowsHtml;
        } else {
            elements.temporalTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">No temporal evaluations recorded.</div>
                    </td>
                </tr>
            `;
        }
    }

    async function loadDocumentsAndClaims(bidId) {
        try {
            const res = await fetch(`/api/bids/${bidId}/documents`);
            if (res.ok) {
                const docs = await res.json();
                state.documents = Array.isArray(docs) ? docs : [];
                renderDocumentsTable(state.documents);

                if (state.documents.length > 0) {
                    updateArchNode('nodeDoc', 'nodeStatusDoc', `${state.documents.length} Uploaded`, 'completed-node');
                    await loadClaimsAcrossDocuments(state.documents);
                } else {
                    updateArchNode('nodeDoc', 'nodeStatusDoc', '0 Documents', '');
                    renderClaimsTable([]);
                }
            } else {
                state.documents = [];
                renderDocumentsTable([]);
                renderClaimsTable([]);
                updateArchNode('nodeDoc', 'nodeStatusDoc', '0 Documents', '');
            }
        } catch (err) {
            state.documents = [];
            renderDocumentsTable([]);
            renderClaimsTable([]);
            updateArchNode('nodeDoc', 'nodeStatusDoc', 'Error', 'alert-node');
        }
    }

    function renderDocumentsTable(docs) {
        if (!docs || docs.length === 0) {
            elements.documentsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">No documents have been uploaded for this bid.</div>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        docs.forEach(doc => {
            const shortHash = doc.sha256Hash ? (doc.sha256Hash.substring(0, 12) + '...') : '—';
            const primaryName = doc.originalFileName || doc.fileName || 'Document';
            const secondaryName = (doc.originalFileName && doc.fileName && doc.originalFileName !== doc.fileName)
                ? `<div class="doc-storage-name" title="Storage path">${escapeHtml(doc.fileName)}</div>`
                : '';

            rowsHtml += `
                <tr>
                    <td>
                        <div class="doc-display-name">${escapeHtml(primaryName)}</div>
                        ${secondaryName}
                    </td>
                    <td><span class="badge badge-gray">${escapeHtml(doc.documentType || 'OTHER')}</span></td>
                    <td>${renderStatusBadge(doc.extractionStatus)}</td>
                    <td>${renderStatusBadge(doc.integrityStatus)}</td>
                    <td class="font-mono" title="${escapeHtml(doc.sha256Hash || '')}">${shortHash}</td>
                    <td>${formatDate(doc.uploadedAt || doc.createdAt)}</td>
                    <td>
                        <button type="button" class="btn btn-sm btn-secondary view-doc-btn" data-doc-id="${doc.documentId}">
                            View Details
                        </button>
                    </td>
                </tr>
            `;
        });
        elements.documentsTableBody.innerHTML = rowsHtml;

        elements.documentsTableBody.querySelectorAll('.view-doc-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const docId = btn.getAttribute('data-doc-id');
                const doc = state.documents.find(d => d.documentId === docId);
                if (doc) showDocumentModal(doc);
            });
        });
    }

    async function loadClaimsAcrossDocuments(docs) {
        const claimsList = [];
        const docNameMap = new Map();
        docs.forEach(d => docNameMap.set(d.documentId, d.fileName || d.originalFileName || 'Document'));

        for (const doc of docs) {
            try {
                const res = await fetch(`/api/documents/${doc.documentId}/claims`);
                if (res.ok) {
                    const claims = await res.json();
                    if (Array.isArray(claims)) {
                        claims.forEach(c => c._docName = docNameMap.get(doc.documentId));
                        claimsList.push(...claims);
                    }
                }
            } catch (err) {
                console.error(`Error loading claims for doc ${doc.documentId}:`, err);
            }
        }

        state.claims = claimsList;
        renderClaimsTable(claimsList);

        if (claimsList.length > 0) {
            updateArchNode('nodeClaim', 'nodeStatusClaim', `${claimsList.length} Extracted`, 'completed-node');
        } else {
            updateArchNode('nodeClaim', 'nodeStatusClaim', '0 Extracted', '');
        }
    }

    function renderClaimsTable(claims) {
        if (!claims || claims.length === 0) {
            elements.claimsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">No extracted claims available for this bid.</div>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        claims.forEach(c => {
            const confPct = c.confidence != null ? `${(c.confidence * 100).toFixed(0)}%` : '—';
            rowsHtml += `
                <tr>
                    <td><strong>${escapeHtml(c.fieldName || '—')}</strong></td>
                    <td class="font-mono">${escapeHtml(c.fieldValue || '—')}</td>
                    <td class="font-mono">${escapeHtml(c.normalizedValue || '—')}</td>
                    <td>${confPct}</td>
                    <td><span class="badge badge-gray">${escapeHtml(c.extractionMethod || 'REGEX')}</span></td>
                    <td>${escapeHtml(c._docName || c.documentId ? (c._docName || c.documentId.substring(0, 8) + '...') : '—')}</td>
                    <td>${c.sourcePage != null ? `Page ${c.sourcePage}` : '—'}</td>
                </tr>
            `;
        });
        elements.claimsTableBody.innerHTML = rowsHtml;
    }

    async function loadEvidence(bidId) {
        try {
            const res = await fetch(`/api/bids/${bidId}/evidence`);
            if (res.ok) {
                const evidenceList = await res.json();
                state.evidenceItems = Array.isArray(evidenceList) ? evidenceList : [];
                renderEvidenceTable(state.evidenceItems);
                renderEvidenceGraphTopology(state.evidenceItems);

                if (state.evidenceItems.length > 0) {
                    updateArchNode('nodeEvidence', 'nodeStatusEvidence', `${state.evidenceItems.length} Nodes`, 'completed-node');

                    const verifiedCount = state.evidenceItems.filter(e => e.verificationStatus === 'VERIFIED').length;
                    const mismatchCount = state.evidenceItems.filter(e => e.verificationStatus === 'MISMATCH').length;
                    const verifClass = mismatchCount > 0 ? 'alert-node' : (verifiedCount > 0 ? 'completed-node' : 'active-node');
                    updateArchNode('nodeVerif', 'nodeStatusVerif', `${verifiedCount} Verified`, verifClass);
                } else {
                    updateArchNode('nodeEvidence', 'nodeStatusEvidence', '0 Nodes', '');
                    updateArchNode('nodeVerif', 'nodeStatusVerif', 'Pending', '');
                }
            } else {
                state.evidenceItems = [];
                renderEvidenceTable([]);
                renderEvidenceGraphTopology([]);
                updateArchNode('nodeEvidence', 'nodeStatusEvidence', '0 Nodes', '');
                updateArchNode('nodeVerif', 'nodeStatusVerif', 'Pending', '');
            }
        } catch (err) {
            state.evidenceItems = [];
            renderEvidenceTable([]);
            renderEvidenceGraphTopology([]);
            updateArchNode('nodeEvidence', 'nodeStatusEvidence', 'Error', 'alert-node');
        }
    }

    function renderEvidenceTable(items) {
        if (!items || items.length === 0) {
            elements.evidenceTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="table-empty-cell">
                        <div class="empty-state-inline">No evidence records available.</div>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        items.forEach(ev => {
            const confPct = ev.confidence != null ? `${(ev.confidence * 100).toFixed(0)}%` : '—';
            const shortProv = ev.provenanceHash ? (ev.provenanceHash.substring(0, 12) + '...') : '—';

            rowsHtml += `
                <tr>
                    <td><strong>${escapeHtml(ev.evidenceType || 'REGISTRY')}</strong></td>
                    <td><span class="badge badge-gray">${escapeHtml(ev.sourceType || ev.sourceSystem || '—')}</span></td>
                    <td>${escapeHtml(ev.attribute || ev.subject || '—')}</td>
                    <td class="font-mono">${escapeHtml(ev.value || '—')}</td>
                    <td>${renderStatusBadge(ev.verificationStatus)}</td>
                    <td>${confPct}</td>
                    <td class="font-mono" title="${escapeHtml(ev.provenanceHash || '')}">${shortProv}</td>
                </tr>
            `;
        });
        elements.evidenceTableBody.innerHTML = rowsHtml;
    }

    function renderEvidenceGraphTopology(items) {
        if (!items || items.length === 0) {
            elements.evidenceEmptyState.classList.remove('hidden');
            elements.evidenceGrid.classList.add('hidden');
            elements.evidenceGrid.innerHTML = '';
            return;
        }

        elements.evidenceEmptyState.classList.add('hidden');
        elements.evidenceGrid.classList.remove('hidden');

        let cardsHtml = '';
        items.forEach(item => {
            let relsHtml = '';
            if (item.relationships && item.relationships.length > 0) {
                item.relationships.forEach(rel => {
                    const relTypeClass = rel.relationshipType === 'VERIFIED_BY' ? 'text-green' : 'text-red';
                    relsHtml += `
                        <div class="rel-item">
                            <span class="font-mono">CLAIM</span>
                            <span>──[</span>
                            <strong class="${relTypeClass}">${rel.relationshipType}</strong>
                            <span>]──▶</span>
                            <span class="font-mono">EVIDENCE</span>
                        </div>
                        <div style="font-size: 0.7rem; color: var(--text-muted); margin-bottom: 0.35rem;">
                            Reason: ${escapeHtml(rel.reason || 'Validated against statutory registry')}
                        </div>
                    `;
                });
            } else {
                relsHtml = '<div style="font-size: 0.75rem; color: var(--text-muted);">No graph edges attached.</div>';
            }

            cardsHtml += `
                <div class="evidence-card-item">
                    <div class="evidence-card-header">
                        <span class="evidence-attr">${escapeHtml(item.attribute || item.subject || 'Evidence Node')}</span>
                        ${renderStatusBadge(item.verificationStatus)}
                    </div>
                    <div class="evidence-detail-row">
                        <span class="evidence-detail-label">Source Type:</span>
                        <span class="evidence-detail-val">${escapeHtml(item.sourceType || '—')}</span>
                    </div>
                    <div class="evidence-detail-row">
                        <span class="evidence-detail-label">Observed Val:</span>
                        <span class="evidence-detail-val font-mono">${escapeHtml(item.value || '—')}</span>
                    </div>
                    <div class="evidence-detail-row">
                        <span class="evidence-detail-label">Confidence:</span>
                        <span class="evidence-detail-val">${item.confidence != null ? (item.confidence * 100).toFixed(0) + '%' : '—'}</span>
                    </div>
                    <div class="evidence-rel-box">
                        <div style="font-weight: 700; margin-bottom: 0.25rem;">Graph Topology:</div>
                        ${relsHtml}
                    </div>
                </div>
            `;
        });

        elements.evidenceGrid.innerHTML = cardsHtml;
    }

    async function loadEntityResolution(bidderId) {
        try {
            const res = await fetch(`/api/entity-resolution/bidders/${bidderId}/resolve`, { method: 'POST' });
            if (res.ok) {
                const data = await res.json();
                state.entityResolutionResult = data;
                elements.entityMatchStatusBadge.innerHTML = renderStatusBadge(data.matchStatus);
                elements.entityConfidenceValue.textContent = data.confidence != null ? `${(data.confidence * 100).toFixed(0)}%` : '—';
                elements.entityExplanationText.textContent = data.explanation || 'Identity resolution evaluated across statutory registries.';

                const nodeClass = data.matchStatus === 'MATCH' ? 'completed-node' : (data.matchStatus === 'MISMATCH' ? 'alert-node' : 'active-node');
                updateArchNode('nodeEntity', 'nodeStatusEntity', data.matchStatus || 'MATCH', nodeClass);
            } else {
                elements.entityMatchStatusBadge.innerHTML = '<span class="badge badge-gray">—</span>';
                elements.entityConfidenceValue.textContent = '—';
                elements.entityExplanationText.textContent = 'Entity resolution has not been evaluated.';
                updateArchNode('nodeEntity', 'nodeStatusEntity', 'Pending', '');
            }
        } catch {
            elements.entityMatchStatusBadge.innerHTML = '<span class="badge badge-gray">—</span>';
            elements.entityConfidenceValue.textContent = '—';
            elements.entityExplanationText.textContent = 'Entity resolution service unavailable.';
            updateArchNode('nodeEntity', 'nodeStatusEntity', 'Offline', '');
        }
    }

    async function triggerVerification() {
        if (!state.activeBidId) return;
        setSpinner(elements.spinnerVerification, true);
        elements.btnRunVerification.disabled = true;

        try {
            const res = await fetch(`/api/bids/${state.activeBidId}/verify`, { method: 'POST' });
            if (!res.ok) throw new Error(`Status ${res.status}`);
            const data = await res.json();
            showNotification(`Registry verification complete: ${data.verifiedCount} verified, ${data.mismatchCount} mismatches.`, data.mismatchCount > 0 ? 'warning' : 'success');
            await loadEvidence(state.activeBidId);
            await loadRisk(state.activeBidId);
        } catch (err) {
            showNotification(`Verification execution failed: ${err.message}`, 'error');
        } finally {
            setSpinner(elements.spinnerVerification, false);
            elements.btnRunVerification.disabled = false;
        }
    }

    async function triggerCompliance() {
        if (!state.activeBidId) return;
        setSpinner(elements.spinnerCompliance, true);
        elements.btnEvaluateCompliance.disabled = true;

        try {
            const res = await fetch(`/api/bids/${state.activeBidId}/compliance/evaluate`, { method: 'POST' });
            if (!res.ok) throw new Error(`Status ${res.status}`);
            const data = await res.json();
            showNotification(`Compliance evaluated: ${data.overallStatus} (${data.compliancePercentage != null ? data.compliancePercentage.toFixed(1) + '%' : '—'})`, data.overallStatus === 'COMPLIANT' ? 'success' : 'warning');
            renderCompliance(data);
            await loadBidsMatrixAndSummary();
        } catch (err) {
            showNotification(`Compliance evaluation failed: ${err.message}`, 'error');
        } finally {
            setSpinner(elements.spinnerCompliance, false);
            elements.btnEvaluateCompliance.disabled = false;
        }
    }

    async function triggerTemporal() {
        if (!state.activeBidId) return;
        setSpinner(elements.spinnerTemporal, true);
        elements.btnRunTemporal.disabled = true;

        try {
            const res = await fetch(`/api/bids/${state.activeBidId}/temporal/evaluate`, { method: 'POST' });
            if (!res.ok) throw new Error(`Status ${res.status}`);
            const data = await res.json();
            showNotification(`Temporal check complete: ${data.validCount} valid, ${data.expiredCount} expired.`, data.expiredCount > 0 ? 'warning' : 'success');
            renderTemporal(data);
            await loadRisk(state.activeBidId);
        } catch (err) {
            showNotification(`Temporal check failed: ${err.message}`, 'error');
        } finally {
            setSpinner(elements.spinnerTemporal, false);
            elements.btnRunTemporal.disabled = false;
        }
    }

    async function triggerRisk() {
        if (!state.activeBidId) return;
        setSpinner(elements.spinnerRisk, true);
        elements.btnEvaluateRisk.disabled = true;

        try {
            const res = await fetch(`/api/bids/${state.activeBidId}/risk/evaluate`, { method: 'POST' });
            if (!res.ok) throw new Error(`Status ${res.status}`);
            const data = await res.json();
            showNotification(`Risk evaluated: ${data.decisionRecommendation} (Score: ${data.overallRiskScore} / 100)`, data.decisionRecommendation === 'RECOMMEND' ? 'success' : 'warning');
            await loadRisk(state.activeBidId);
            await loadBidsMatrixAndSummary();
        } catch (err) {
            showNotification(`Risk evaluation failed: ${err.message}`, 'error');
        } finally {
            setSpinner(elements.spinnerRisk, false);
            elements.btnEvaluateRisk.disabled = false;
        }
    }

    function updateArchNode(nodeId, statusId, statusText, cssClass) {
        const node = document.getElementById(nodeId);
        const statusEl = document.getElementById(statusId);
        if (node) {
            node.className = `arch-node ${cssClass || ''}`;
        }
        if (statusEl) {
            statusEl.textContent = statusText;
        }
    }

    function resetArchitectureFlow() {
        const nodes = [
            { n: 'nodeDoc', s: 'nodeStatusDoc', t: 'Awaiting Bid' },
            { n: 'nodeClaim', s: 'nodeStatusClaim', t: 'Awaiting Bid' },
            { n: 'nodeEvidence', s: 'nodeStatusEvidence', t: 'Awaiting Bid' },
            { n: 'nodeVerif', s: 'nodeStatusVerif', t: 'Awaiting Bid' },
            { n: 'nodeEntity', s: 'nodeStatusEntity', t: 'Awaiting Bid' },
            { n: 'nodeComp', s: 'nodeStatusComp', t: 'Awaiting Bid' },
            { n: 'nodeTemporal', s: 'nodeStatusTemporal', t: 'Awaiting Bid' },
            { n: 'nodeRisk', s: 'nodeStatusRisk', t: 'Awaiting Bid' },
            { n: 'nodeDecision', s: 'nodeStatusDecision', t: 'Awaiting Bid' }
        ];

        nodes.forEach(item => {
            const node = document.getElementById(item.n);
            const status = document.getElementById(item.s);
            if (node) node.className = 'arch-node';
            if (status) status.textContent = item.t;
        });
    }

    function renderEmptyTabDossier() {
        renderCompliance(null);
        renderTemporal(null);
        renderRiskDimensions([]);
        renderDocumentsTable([]);
        renderClaimsTable([]);
        renderEvidenceTable([]);
        renderEvidenceGraphTopology([]);
        elements.entityMatchStatusBadge.innerHTML = '<span class="badge badge-gray">—</span>';
        elements.entityConfidenceValue.textContent = '—';
        elements.entityExplanationText.textContent = 'Select a bid to inspect entity resolution results.';
    }

    function renderStatusBadge(status, id) {
        if (!status) return `<span class="badge badge-gray" ${id ? `id="${id}"` : ''}>—</span>`;

        const s = status.toString().toUpperCase();
        let badgeClass = 'badge-gray';

        if (['VERIFIED', 'COMPLIANT', 'RECOMMEND', 'MATCH', 'VALID_AT_BID_DATE', 'VALID', 'LOW', 'ACTIVE', 'SUBMITTED'].includes(s)) {
            badgeClass = 'badge-positive';
        } else if (['REVIEW', 'PENDING_HUMAN_REVIEW', 'PENDING', 'UNVERIFIED', 'PROBABLE_MATCH', 'PARTIALLY_COMPLIANT', 'NOT_YET_VALID_AT_BID_DATE', 'FUTURE_VALIDITY', 'MEDIUM'].includes(s)) {
            badgeClass = 'badge-warning';
        } else if (['MISMATCH', 'NON_COMPLIANT', 'HIGH_RISK', 'EXPIRED_AT_BID_DATE', 'EXPIRED', 'ISSUED_AFTER_BID', 'CONFLICTING_DATES', 'CONTRADICTORY', 'CRITICAL', 'HIGH', 'HARD_FAIL'].includes(s)) {
            badgeClass = 'badge-negative';
        } else if (['INFO', 'INFORMATIONAL', 'EXTRACTED', 'PROCESSED'].includes(s)) {
            badgeClass = 'badge-blue';
        }

        return `<span class="badge ${badgeClass}" ${id ? `id="${id}"` : ''}>${escapeHtml(s)}</span>`;
    }

    function formatDate(dt) {
        if (!dt) return '—';
        try {
            const d = new Date(dt);
            if (isNaN(d.getTime())) return dt.toString();
            return d.toLocaleString('en-IN', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dt.toString();
        }
    }

    function formatDateOnly(dt) {
        if (!dt) return '—';
        try {
            const d = new Date(dt);
            if (isNaN(d.getTime())) return dt.toString();
            return d.toLocaleDateString('en-IN', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
        } catch {
            return dt.toString();
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function setSpinner(spinnerEl, active) {
        if (!spinnerEl) return;
        if (active) spinnerEl.classList.remove('hidden');
        else spinnerEl.classList.add('hidden');
    }

    function showNotification(message, type = 'info', timeoutMs = 6000) {
        elements.notificationMessage.textContent = message;
        elements.notificationBanner.className = `notification-banner notification-${type}`;
        elements.notificationBanner.classList.remove('hidden');

        if (type === 'success') elements.notificationIcon.textContent = '✓';
        else if (type === 'error') elements.notificationIcon.textContent = '⚠';
        else if (type === 'warning') elements.notificationIcon.textContent = '⚡';
        else elements.notificationIcon.textContent = 'ℹ';

        if (timeoutMs > 0) {
            setTimeout(() => {
                elements.notificationBanner.classList.add('hidden');
            }, timeoutMs);
        }
    }

    function showDocumentModal(doc) {
        elements.modalDocTitle.textContent = doc.originalFileName || doc.fileName || 'Document Cryptographic Metadata';
        const fileSizeKb = doc.fileSize ? (doc.fileSize / 1024).toFixed(1) + ' KB' : '—';
        const hasDiffStorageName = doc.fileName && doc.originalFileName && doc.fileName !== doc.originalFileName;

        elements.modalDocBody.innerHTML = `
            <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                <div class="overview-item">
                    <div class="overview-label">Document ID</div>
                    <div class="overview-value font-mono">${escapeHtml(doc.documentId || '—')}</div>
                </div>
                <div class="overview-item">
                    <div class="overview-label">Original File Name</div>
                    <div class="overview-value"><strong>${escapeHtml(doc.originalFileName || doc.fileName || '—')}</strong></div>
                </div>
                ${hasDiffStorageName ? `
                <div class="overview-item">
                    <div class="overview-label">Storage / Generated File Name</div>
                    <div class="overview-value font-mono" style="font-size: 0.85rem; color: var(--text-secondary);">${escapeHtml(doc.fileName)}</div>
                </div>
                ` : ''}
                <div class="overview-item">
                    <div class="overview-label">Content Type / MIME</div>
                    <div class="overview-value">${escapeHtml(doc.contentType || 'application/pdf')}</div>
                </div>
                <div class="overview-item">
                    <div class="overview-label">File Size</div>
                    <div class="overview-value">${fileSizeKb}</div>
                </div>
                <div class="overview-item">
                    <div class="overview-label">SHA-256 Provenance Hash</div>
                    <div class="overview-value font-mono" style="font-size: 0.8rem; word-break: break-all;">
                        ${escapeHtml(doc.sha256Hash || '—')}
                    </div>
                </div>
                <div class="overview-item">
                    <div class="overview-label">Extraction & Integrity Status</div>
                    <div class="overview-value" style="display: flex; gap: 0.5rem; margin-top: 0.25rem;">
                        ${renderStatusBadge(doc.extractionStatus)}
                        ${renderStatusBadge(doc.integrityStatus)}
                    </div>
                </div>
                <div class="overview-item">
                    <div class="overview-label">Uploaded Timestamp</div>
                    <div class="overview-value">${formatDate(doc.uploadedAt || doc.createdAt)}</div>
                </div>
            </div>
        `;
        elements.docDetailsModal.classList.remove('hidden');
    }

    if (elements.loadDemoDatasetBtn) {
        elements.loadDemoDatasetBtn.addEventListener('click', handleLoadDemoDataset);
    }
    if (elements.resetDemoDatasetBtn) {
        elements.resetDemoDatasetBtn.addEventListener('click', handleResetDemoDataset);
    }

    if (elements.btnRefreshMatrix) {
        elements.btnRefreshMatrix.addEventListener('click', () => {
            loadBidsMatrixAndSummary();
            showNotification('Bid matrix and summary cards refreshed from backend.', 'info', 3000);
        });
    }

    elements.loadBidBtn.addEventListener('click', () => {
        selectBidForAnalysis(elements.bidIdInput.value);
    });

    elements.bidIdInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            selectBidForAnalysis(elements.bidIdInput.value);
        }
    });

    elements.clearBidBtn.addEventListener('click', clearActiveBid);

    elements.btnRunVerification.addEventListener('click', triggerVerification);
    elements.btnEvaluateCompliance.addEventListener('click', triggerCompliance);
    elements.btnRunTemporal.addEventListener('click', triggerTemporal);
    elements.btnEvaluateRisk.addEventListener('click', triggerRisk);
    elements.btnRefreshAssessment.addEventListener('click', () => {
        if (state.activeBidId) {
            refreshActiveBidDetailedLayers(state.activeBidId);
            showNotification('Active bid data refreshed from backend.', 'info', 3000);
        }
    });

    if (elements.decisionFullReasonToggle && elements.fullReasonToggleLabel) {
        elements.decisionFullReasonToggle.addEventListener('toggle', () => {
            if (elements.decisionFullReasonToggle.open) {
                elements.fullReasonToggleLabel.textContent = 'Hide Full Deterministic Reason';
            } else {
                elements.fullReasonToggleLabel.textContent = 'View Full Deterministic Reason';
            }
        });
    }

    document.querySelectorAll('.detail-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            document.querySelectorAll('.detail-tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.detail-tab-content').forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const targetContent = document.getElementById(targetTab);
            if (targetContent) targetContent.classList.add('active');
        });
    });

    document.querySelectorAll('.arch-node').forEach(node => {
        node.addEventListener('click', () => {
            const targetTab = node.getAttribute('data-target');
            if (!targetTab) return;

            if (targetTab === 'decisionSection') {
                const decSec = document.getElementById('decisionSection');
                if (decSec) decSec.scrollIntoView({ behavior: 'smooth' });
                return;
            }

            const tabBtn = document.querySelector(`.detail-tab-btn[data-tab="${targetTab}"]`);
            if (tabBtn) {
                tabBtn.click();
                const detailSec = document.getElementById('bidDetailSection');
                if (detailSec) detailSec.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });

    const closeModal = () => elements.docDetailsModal.classList.add('hidden');
    elements.closeDocModalBtn.addEventListener('click', closeModal);
    elements.closeDocModalFooterBtn.addEventListener('click', closeModal);
    elements.docDetailsModal.addEventListener('click', (e) => {
        if (e.target === elements.docDetailsModal) closeModal();
    });

    elements.closeNotificationBtn.addEventListener('click', () => {
        elements.notificationBanner.classList.add('hidden');
    });

    document.querySelectorAll('.app-nav .nav-link').forEach(link => {
        link.addEventListener('click', () => {
            document.querySelectorAll('.app-nav .nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        });
    });

    checkHealth();
    refreshAllDashboardData();
    setInterval(checkHealth, 30000);
});
