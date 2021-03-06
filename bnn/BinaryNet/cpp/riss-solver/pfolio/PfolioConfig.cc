/*********************************************************************************[PfolioConfig.cc]

Copyright (c) 2012-2013, Norbert Manthey, LGPL v2, see LICENSE
**************************************************************************************************/

#include "pfolio/PfolioConfig.h"

namespace Riss
{

static const char* _cat = "PFOLIO";

PfolioConfig::PfolioConfig(const std::string& presetOptions)  // add new options here!
    :
    Config(&configOptions, presetOptions)

    , opt_proofCounting("PFOLIO - PROOF", "pc",  "enable avoiding duplicate clauses in the pfolio DRUP proof", true, optionListPtr)
    , opt_verboseProof("PFOLIO - PROOF", "pv",  "verbose proof (2=with comments to clause authors,1=comments by master only, 0=off)", 1, IntRange(0, 2), optionListPtr)
    , opt_internalProofCheck("PFOLIO - PROOF", "pic", "use internal proof checker during run time", false, optionListPtr)
    , opt_verbosePfolio("PFOLIO - PROOF", "ppv", "verbose pfolio execution", false, optionListPtr)

    , threads("PFOLIO - INIT", "threads", "Number of threads to be used by the parallel solver.", 2, IntRange(1, 64), optionListPtr)
    , opt_defaultSetup("PFOLIO - INIT", "psetup", "how to setup client solvers(e.g. PLAIN)", 0, optionListPtr)
    , opt_incarnationSetups("PFOLIO - INIT", "pIncSetup", "incarnation configurations [N]confign[N+1]configN+1", 0, optionListPtr)
    , addExtraSetup("PFOLIO - INIT", "addSetup", "add pIncSetup configuration to preset (indepdently of preset)", true, optionListPtr)
    , opt_ppconfig("PFOLIO - INIT", "ppconfig", "the configuration to be used for the simplifier", 0, optionListPtr)
    , opt_allIncPresets("PFOLIO - INIT", "pAllSetup", "add to all incarnations (after other setups)", 0, optionListPtr)

    , opt_storageSize("PFOLIO - INIT", "storageSize", "Number of clauses in one ring buffer (0 => 4000 x threads)", 0, IntRange(0, INT32_MAX), optionListPtr)

    , opt_share("SEND", "ps", "enable clause sharing for all clients", true, optionListPtr)
    , opt_receive("SEND", "pr", "enable receiving clauses for all clients", true, optionListPtr)

    , opt_protectAssumptions("SEND", "protectA", "should assumption variables not be considered for calculating send-limits", false, optionListPtr)
    , opt_sendSize("SEND", "sendSize", "Minimum Lbd of clauses to send  (also start value)", 10, DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_sendLbd("SEND", "sendLbd", "initial value, also minimum limit (first checks size)", 5, DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_sendMaxSize("SEND", "sendMaxSize", "upper bound for clause size (larger clause is never shared)", 128, DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_sendMaxLbd("SEND",  "sendMaxLbd",  "upper bound for clause size (larger clause is never shared)", 32,  DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_sizeChange("SEND",  "sizeChange",  "set to value greater than 0 to see dynamic limit changes! (e.g. 0.05)", 0,   DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_lbdChange("SEND",   "lbdChange",   "set to value greater than 0 to see dynamic limit changes! (e.g. 0.02)", 0,   DoubleRange(0, true, HUGE_VAL, true), optionListPtr)
    , opt_sendRatio("SEND",   "sendRatio",   "", 0.1, DoubleRange(0, true, 1, true), optionListPtr)

    , opt_doBumpClauseActivity("SEND", "bumpSentCA", "bump activity of received clauses", false, optionListPtr)
    , opt_checkLiterals("SEND", "checkLits", "control allowing sending and receiving information based on literal instead of variables", false, optionListPtr)
    , opt_useDynamicLimits("SEND", "dynLimits", "update sharing limits dynamically", false, optionListPtr)
    , opt_sendEquivalences("SEND", "shareEE", "share equivalent literals", true, optionListPtr)

{
    if (defaultPreset.size() != 0) {
//     DOUT( std::cerr << "c set preset for pfolio: " << defaultPreset << std::endl;  );
        setPreset(defaultPreset);
    }    // set configuration options immediately
}

} // namespace Riss
