import style from "./footer.module.scss";
import { Link, useLocation } from "react-router-dom";
import {
  COMMUNITY_GROUP_URL,
  ONCOTREE_SWAGGER_URL,
  PageRoutes,
} from "../../shared/constants";

export default function Footer() {
  const location = useLocation();

  return (
    <div
      className={style.footer}
      style={
        location.pathname !== PageRoutes.HOME ? { display: "none" } : undefined
      }
    >
      <div className={style["footer-container"]}>
        <div className={style["mobile-citation"]}>
          <span className={style["mobile-hidden"]}>
            {" "}
            When using OncoTree, please cite: <br />
          </span>
          <Link
            style={{ pointerEvents: "auto" }}
            to={"https://ascopubs.org/doi/10.1200/CCI.20.00108"}
            target="_blank"
          >
            <i>Kundra et al., JCO Clinical Cancer Informatics</i> 2021
          </Link>
        </div>
        <div style={{ minWidth: 50 }} />
        <div
          style={{
            pointerEvents: "auto",
            textAlign: "center",
            display: "flex",
            alignItems: "center",
          }}
        >
          <div
            className={style["mobile-community-group"]}
            style={{ marginRight: 25 }}
          >
            <a
              href={COMMUNITY_GROUP_URL}
              target="_blank"
              style={{ pointerEvents: "auto" }}
            >
              Community Group
            </a>
          </div>
          <a
            href={ONCOTREE_SWAGGER_URL}
            className={style["mobile-hidden"]}
            target="_blank"
            style={{ pointerEvents: "auto" }}
          >
            API
          </a>
        </div>
      </div>
    </div>
  );
}
