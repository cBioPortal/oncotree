import {
  FontAwesomeIcon,
  FontAwesomeIconProps,
} from "@fortawesome/react-fontawesome";
import styles from "./icon-button.module.scss";

export default function IconButton(props: FontAwesomeIconProps) {
  const { className = styles["icon-button"], size = "xl", ...rest } = props;

  return <FontAwesomeIcon {...rest} className={className} size={size} />;
}
