package api

type Version struct {
	ApiIdentifier string `json:"api_identifier"`
	Description   string `json:"description"`
	ReleaseDate   string `json:"release_date"`
	Visible       bool   `json:"visible"`
}

func hardcodedVersions() []Version {
	return []Version{
		{
			ApiIdentifier: "oncotree_legacy_1.1",
			Description:   "This is the closest match in TopBraid for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)",
			ReleaseDate:   "2016-03-28",
			Visible:       false,
		},
		{
			ApiIdentifier: "oncotree_candidate_release",
			Description:   "This version of the OncoTree reflects upcoming changes which have been approved for the next public release of oncotree. It also includes a small number of nodes which will not be included in the next public release (see the news page for more details). The next public release may possibly include additional oncotree nodes, if approved.",
			ReleaseDate:   "2021-11-03",
			Visible:       true,
		},
		{
			ApiIdentifier: "oncotree_development",
			Description:   "Latest OncoTree under development (subject to <b class=text-danger>change without notice</b>)",
			ReleaseDate:   "2021-11-04",
			Visible:       true,
		},
	}
}
