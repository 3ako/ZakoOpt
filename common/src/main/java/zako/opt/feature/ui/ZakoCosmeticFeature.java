package zako.opt.feature.ui;
import zako.opt.feature.*;
@FeatureData(key = "zako_cosmetic", group = FeatureGroup.INTERFACE, visual = true, enabled = false, mixins = {"entity.ZakoAvatarMixin"})
public class ZakoCosmeticFeature extends Feature {
    @Setting(key = "zako_logo") @Range(min = 0, max = 6, unit = "zakoopt.zako_logo_value") public int logo = 0;
    @Setting(key = "zako_white_shirt") public boolean white = false;
}
